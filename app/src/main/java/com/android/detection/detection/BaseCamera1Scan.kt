package com.android.detection.detection

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.detection.detection.analyze.Analyzer
import com.android.detection.detection.analyze.Analyzer.OnAnalyzeListener
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min


/**
 * 相机扫描基类；[BaseCamera1Scan] 为 [CameraScan] 的默认实现
 */
class BaseCamera1Scan<T>(
    private val mContext: Context,
    private val mLifecycleOwner: LifecycleOwner,
    private val surfaceView: SurfaceView
) : CameraScan<T>() {
    /**
     * 分析器
     */
    private var mAnalyzer: Analyzer<T?>? = null

    /**
     * 分析结果
     */
    private var mResultLiveData: MutableLiveData<AnalyzeResult<T?>?>? = null

    /**
     * 扫描结果回调
     */
    private var mOnScanResultCallback: OnScanResultCallback<T?>? = null

    private var sensorManager: SensorManager? = null
    private var gravitySensor: Sensor? = null
    private var sensorEventListener: SensorEventListener? = null
    private var gravityValue = 0
    private var flashFlag = false


    private var cameraManager: CameraManager? = null
    private var screenWidth = 0
    private var screenHeight = 0

    private var camera: Camera? = null
    private var holder: SurfaceHolder? = null
    private var callback: SurfaceHolder.Callback? = null
    private var isOpen = false

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

        sensorManager = mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gravitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GRAVITY)
        sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(sensorEvent: SensorEvent) {
                if (sensorEvent.sensor.type == Sensor.TYPE_GRAVITY) {
                    val x = sensorEvent.values[0]
                    val y = sensorEvent.values[1]
                    if (x > 3) gravityValue = 3
                    else if (x < -3) gravityValue = 1
                    else if (y > 3) gravityValue = 0
                    else if (y < -3) gravityValue = 2
                    else if (x >= -1.0 && x <= 1.0 && y >= -1.0 && y <= 1.0) gravityValue = 0
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, i: Int) {
            }
        }

        cameraManager = mContext.getSystemService(CAMERA_SERVICE) as CameraManager
    }


    private var lastPictureSize: Camera.Size? = null
    fun openCamera() {
        try {
            if (camera != null) {
                lastPictureSize = null
                val windowManager =
                    mContext.getSystemService(WINDOW_SERVICE) as WindowManager
                val display: Display = windowManager.defaultDisplay
                val recodeRotation = display.rotation
                if (recodeRotation == 0) camera?.setDisplayOrientation(90)
                if (recodeRotation == 1) camera?.setDisplayOrientation(0)
                if (recodeRotation == 2) camera?.setDisplayOrientation(90)
                if (recodeRotation == 3) camera?.setDisplayOrientation(0)

                val params = camera?.parameters
                params?.pictureFormat = PixelFormat.JPEG

                // 获取预览分辨率列表
                val previewSizes = params!!.supportedPreviewSizes.sortedByDescending { it.width }
                // 获取拍照分辨率列表
//                val pictureSizes = params.supportedPictureSizes.sortedByDescending { it.width }
//                previewSizes.forEach {
//                    println("这里的预览大小时》》》》   ${it.width}    ${it.height}")
//                }
//
//                pictureSizes.forEach {
//                    println("这里的paizhao1chi大小时》》》》   ${it.width}    ${it.height}")
//                }

                // 遍历这些列表以找到你想要的分辨率
                for (previewSize in previewSizes) {
                    if ((previewSize.width / previewSize.height.toFloat()) == (4 / 3f)) {
                        lastPictureSize = previewSize
                        break
                    }
                }

                if (lastPictureSize != null) {
//                    println("这里分辨率尺寸是》》》》    ${lastPictureSize!!.width}    ${lastPictureSize!!.height}")
                    params.setPreviewSize(lastPictureSize!!.width, lastPictureSize!!.height) //4:3  1920 1440
                    params.setPictureSize(4096, 3072)
                }
                camera?.parameters = params
                camera?.setPreviewCallback(previewCallback)
                camera?.startPreview()
                isOpen = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val previewCallback: PreviewCallback = object : PreviewCallback {
        override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
            // 处理预览数据
            mAnalyzer!!.analyze(data!!, object : OnAnalyzeListener<T?> {
                override fun onSuccess(result: AnalyzeResult<T?>) {
                    mResultLiveData!!.postValue(result)
                }

                override fun onFailure(e: Exception?) {
                    mResultLiveData!!.postValue(null)
                }
            })
        }
    }

    fun closeCamera() {
        try {
            camera?.stopPreview()
            camera?.setPreviewCallback(null)
            camera?.release()
            camera = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @SuppressLint("MissingPermission")
    override fun startCamera() {
        try {
            sensorManager!!.registerListener(
                sensorEventListener,
                gravitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            if (isOpen) return
            callback = object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    try {
                        camera = Camera.open(0)
                        camera?.setPreviewDisplay(holder)
                        openCamera()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    closeCamera()
                }
            }
            holder = surfaceView.holder
            holder?.addCallback(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun takePhoto() {
        var rotationCompensation = 0
        if (gravityValue == 0) rotationCompensation = 90
        else if (gravityValue == 1) rotationCompensation = 180
        else if (gravityValue == 2) rotationCompensation = 270
        else if (gravityValue == 3) rotationCompensation = 0

        val params = camera?.parameters
        if (flashFlag) params?.flashMode = Camera.Parameters.FLASH_MODE_ON
        else params?.flashMode = Camera.Parameters.FLASH_MODE_OFF
        params?.setRotation(rotationCompensation)
        camera?.parameters = params
        camera?.takePicture(null, null, object : Camera.PictureCallback{
            override fun onPictureTaken(p0: ByteArray?, p1: Camera?) {
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "Photo_${System.currentTimeMillis()}")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Camera Detection")
                    }
                    val uri: Uri? = mContext.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                    )
                    val outputStream1: OutputStream? =
                        uri?.let { mContext.contentResolver.openOutputStream(it) }
                    outputStream1?.write(p0)
                    outputStream1?.close()
                    Toast.makeText(mContext, "拍照成功！", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                camera!!.startPreview()
            }
        })
    }

    /**
     * 处理分析结果
     *
     * @param result 分析结果
     */
    @Synchronized
    private fun handleAnalyzeResult(result: AnalyzeResult<T?>?) {
        if (mOnScanResultCallback != null) {
            mOnScanResultCallback!!.onScanResultCallback(result)
        }
    }

    override fun stopCamera() {
        sensorManager!!.unregisterListener(sensorEventListener)
        try {
            closeCamera()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setAnalyzeImage(analyze: Boolean): CameraScan<T> {
        return this
    }

    override fun setAutoStopAnalyze(autoStopAnalyze: Boolean): CameraScan<T> {
        return this
    }

    override fun setAnalyzer(analyzer: Analyzer<T?>?): CameraScan<T> {
        mAnalyzer = analyzer
        return this
    }

    override fun enableTorch(torch: Boolean) {
        flashFlag = torch
    }

    override fun setOnScanResultCallback(callback: OnScanResultCallback<T?>?): CameraScan<T> {
        this.mOnScanResultCallback = callback
        return this
    }


    override fun release() {
        stopCamera()
    }
}
