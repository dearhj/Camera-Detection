package com.android.detection.detection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.camera.core.Camera
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.detection.detection.analyze.Analyzer
import com.android.detection.detection.analyze.Analyzer.OnAnalyzeListener
import kotlin.math.max
import kotlin.math.min

/**
 * 相机扫描基类；[BaseCamera2Scan] 为 [CameraScan] 的默认实现
 */
class BaseCamera2Scan<T>(
    private val mContext: Context,
    private val mLifecycleOwner: LifecycleOwner,
    private val textureView: TextureView
) : CameraScan<T>() {
    /**
     * 分析器
     */
    private var mAnalyzer: Analyzer<T?>? = null

    /**
     * 是否分析
     */
    @Volatile
    private var isAnalyze = true

    /**
     * 是否自动停止分析
     */
    @Volatile
    private var isAutoStopAnalyze = true

    /**
     * 是否已经分析出结果
     */
    @Volatile
    private var isAnalyzeResult = false

    /**
     * 分析结果
     */
    private var mResultLiveData: MutableLiveData<AnalyzeResult<T?>?>? = null

    private var mRotationLiveData: MutableLiveData<Int?>? = null

    /**
     * 扫描结果回调
     */
    private var mOnScanResultCallback: OnScanResultCallback<T?>? = null

    /**
     * 分析监听器
     */
    private var mOnAnalyzeListener: OnAnalyzeListener<T?>? = null

    private var sensorManager: SensorManager? = null
    private var gravitySensor: Sensor? = null
    private var sensorEventListener: SensorEventListener? = null
    private var currentGravityValue = 0
    private var gravityValue = 0


    private var cameraManager: CameraManager? = null
    private var cameraFrontId = "" //前摄1
    private var cameraBackId = "" //后摄0
    private var currentCameraId = "0" //当前ID
    private var hasFrontCamera = false
    private var hasBackCamera = false
    private var previewSizes: Size? = null
    private var screenWidth = 0
    private var screenHeight = 0

    private var imageReader: ImageReader? = null
    private var cameraDevice: CameraDevice? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var captureCallback: CaptureCallback? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    /**
     * 初始化
     */
    private var currentTime = 0L

    init {
        val defaultDisplay =
            (mContext.getSystemService("window") as WindowManager).defaultDisplay
        val displayMetrics = DisplayMetrics()
        defaultDisplay.getRealMetrics(displayMetrics)
        screenWidth =
            max(displayMetrics.widthPixels.toDouble(), displayMetrics.heightPixels.toDouble())
                .toInt()
        screenHeight =
            min(displayMetrics.widthPixels.toDouble(), displayMetrics.heightPixels.toDouble())
                .toInt()
        initData()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initData() {
        mResultLiveData = MutableLiveData<AnalyzeResult<T?>?>()
        mResultLiveData!!.observe(mLifecycleOwner, Observer { result: AnalyzeResult<T?>? ->
            val time = System.currentTimeMillis()
            if ((time - currentTime) > 100) {
                currentTime = time
                handleAnalyzeResult(result)
            } else if (mOnScanResultCallback != null) {
                mOnScanResultCallback!!.onScanResultFailure()
            }
        })
        mRotationLiveData = MutableLiveData<Int?>()
        mRotationLiveData!!.observe(mLifecycleOwner, Observer { rotation: Int? ->
            if (currentGravityValue != rotation) {
                currentGravityValue = rotation!!
                println("这里方向变化了。    $rotation")
                try {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })

        mOnAnalyzeListener = object : OnAnalyzeListener<T?> {
            override fun onSuccess(result: AnalyzeResult<T?>) {
                mResultLiveData!!.postValue(result)
            }

            override fun onFailure(e: Exception?) {
                mResultLiveData!!.postValue(null)
            }
        }

        sensorManager = mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gravitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GRAVITY)
        sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(sensorEvent: SensorEvent) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    val x = sensorEvent.values[0]
                    val y = sensorEvent.values[1]
                    if (x > 3) gravityValue = 3
                    else if (x < -3) gravityValue = 1
                    else if (y > 3) gravityValue = 0
                    else if (y < -3) gravityValue = 2
                    else if (x >= -1.0 && x <= 1.0 && y >= -1.0 && y <= 1.0) gravityValue = 0
//                    mRotationLiveData.postValue(gravityValue);
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, i: Int) {
            }
        }

        cameraManager = mContext.getSystemService(CAMERA_SERVICE) as CameraManager
        textureView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        getCameraInfo()
        chooseBestPreViewSize()

        handlerThread = HandlerThread("ImageReaderThread")
        handlerThread!!.start()
        handler = Handler(handlerThread!!.looper)
        imageReader = ImageReader.newInstance(
            previewSizes!!.width,
            previewSizes!!.height,
            ImageFormat.YUV_420_888,
            2 // 缓冲区数量
        )
        var nextImage = false
        imageReader!!.setOnImageAvailableListener({ reader ->
            if (nextImage) return@setOnImageAvailableListener
            nextImage = true
            val image = reader.acquireLatestImage()
            mAnalyzer!!.analyze(image, object : OnAnalyzeListener<T?> {
                override fun onSuccess(result: AnalyzeResult<T?>) {
                    mResultLiveData!!.postValue(result)
                    nextImage = false
                    image.close()
                }

                override fun onFailure(e: Exception?) {
                    mResultLiveData!!.postValue(null)
                    nextImage = false
                    image.close()
                }
            })
        }, handler)
    }

    private fun getCameraInfo(): Boolean {
        try {
            val cameraIdList = cameraManager?.cameraIdList
            if (cameraIdList?.size == 0) return false
            else {
                for (cameraId in cameraIdList!!) {
                    val characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                        hasFrontCamera = true
                        if (cameraFrontId == "") cameraFrontId = cameraId
                    }
                    if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        hasBackCamera = true
                        if (cameraBackId == "") cameraBackId = cameraId
                    }
                }
                currentCameraId = if (!hasBackCamera) cameraFrontId else cameraBackId
                return hasFrontCamera || hasBackCamera
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun chooseBestPreViewSize() {
        try {
            if (cameraManager == null) return
            //获取摄像头属性描述
            val cameraCharacteristics =
                cameraManager!!.getCameraCharacteristics(currentCameraId)
            //获取摄像头支持的配置属性
            val map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val list = map!!.getOutputSizes(SurfaceTexture::class.java)
            list.forEach { println("这里的帧率是？    ${it.height}   ${it.width}") }
            previewSizes = Size(2560, 1920)
            val layoutParams = textureView.layoutParams
            layoutParams.width = screenHeight
            layoutParams.height = (screenHeight * 1.333333).toInt()
            textureView.layoutParams = layoutParams
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun startCamera() {
        try {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                @RequiresPermission(Manifest.permission.CAMERA)
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    surface.setDefaultBufferSize(width, height)
                    if (cameraManager == null) return
                    cameraManager!!.openCamera(
                        currentCameraId,
                        object : CameraDevice.StateCallback() {
                            override fun onOpened(camera: CameraDevice) {
                                cameraDevice = camera
                                startPreview(surface)
                            }

                            override fun onDisconnected(camera: CameraDevice) {
                                stopCamera()
                            }

                            override fun onError(camera: CameraDevice, error: Int) {
                                stopCamera()
                            }
                        }, null
                    )
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    stopCamera()
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        sensorManager!!.registerListener(
            sensorEventListener,
            gravitySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun startPreview(surface: SurfaceTexture) {
        try {
            if (previewSizes == null) return
            val surface = Surface(surface)
            previewBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            //设置预览输出的界面
            val fpsRange = Range(1, 7)
            previewBuilder?.set<Range<Int>>(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            previewBuilder!!.addTarget(surface)
            previewBuilder!!.addTarget(imageReader!!.surface)
            cameraDevice!!.createCaptureSession(
                listOf(surface, imageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        previewBuilder?.setTag("TAG_PREVIEW")
                        try {
                            cameraCaptureSession?.setRepeatingRequest(
                                previewBuilder!!.build(),
                                null,
                                null
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        session.close()
                        stopCamera()
                    }
                },
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            stopCamera()
        }
    }

    override fun takePhoto() {

    }

    /**
     * 处理分析结果
     *
     * @param result 分析结果
     */
    @Synchronized
    private fun handleAnalyzeResult(result: AnalyzeResult<T?>?) {
        if (isAnalyzeResult || !isAnalyze) {
            return
        }
        isAnalyzeResult = true
        if (isAutoStopAnalyze) {
            isAnalyze = false
        }
        if (mOnScanResultCallback != null) {
            mOnScanResultCallback!!.onScanResultCallback(result)
        }
        isAnalyzeResult = false
    }

    override fun stopCamera() {
        sensorManager!!.unregisterListener(sensorEventListener)
        try {
            //关闭相机
            if (cameraDevice != null) {
                cameraDevice?.close()
                cameraDevice = null
            }
            cameraCaptureSession?.close()
            handler!!.removeCallbacksAndMessages(null)
            handlerThread!!.quitSafely()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setAnalyzeImage(analyze: Boolean): CameraScan<T> {
        isAnalyze = analyze
        return this
    }

    override fun setAutoStopAnalyze(autoStopAnalyze: Boolean): CameraScan<T> {
        isAutoStopAnalyze = autoStopAnalyze
        return this
    }

    override fun setAnalyzer(analyzer: Analyzer<T?>?): CameraScan<T> {
        mAnalyzer = analyzer
        return this
    }

    override fun enableTorch(torch: Boolean) {

    }

    override fun isTorchEnabled(): Boolean {
        return false
    }

    override fun hasFlashUnit(): Boolean {
        return mContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    override fun setOnScanResultCallback(callback: OnScanResultCallback<T?>?): CameraScan<T> {
        this.mOnScanResultCallback = callback
        return this
    }

    override fun getCamera(): Camera? {
        return null
    }

    override fun release() {
        isAnalyze = false
        stopCamera()
    }

    override fun bindFlashlightView(flashlightView: View?): CameraScan<T> {
        return this
    }
}
