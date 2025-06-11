package com.android.detection.detection

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.view.WindowCompat
import com.android.detection.detection.analyze.Analyzer
import com.android.detection.detection.analyze.ObjectDetectionAnalyzer
import com.android.detection.detection.util.PermissionUtils
import com.google.mlkit.vision.objects.DetectedObject
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig


class MainActivity : AppCompatActivity(),
    CameraScan.OnScanResultCallback<MutableList<DetectedObject>> {

    private val requestCodeCamera: Int = 0x86
    private var previewView: PreviewView? = null
    private var mCameraScan: CameraScan<MutableList<DetectedObject>>? = null
    private var objectBoundsView: ObjectBoundsView? = null
    private var tack: Button? = null
    private var picture: Button? = null
    private var showDialog = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_scan)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
        }
        tack = findViewById(R.id.take)
        picture = findViewById(R.id.picture)
        objectBoundsView = findViewById(R.id.object_bound)
        previewView = findViewById<PreviewView?>(R.id.previewView)
        mCameraScan = BaseCameraScan(this, previewView!!)
        mCameraScan!!.setAnalyzer(createAnalyzer()).setOnScanResultCallback(this)
        tack?.setOnClickListener {

        }
        picture?.setOnClickListener {
            showDialog = !showDialog
        }
    }

    override fun onResume() {
        startCamera()
        super.onResume()
    }


    override fun onDestroy() {
        releaseCamera()
        super.onDestroy()
    }

    /**
     * 启动相机预览
     */
    fun startCamera() {
        if (mCameraScan != null) {
            if (PermissionUtils.checkPermission(this, Manifest.permission.CAMERA)) {
                mCameraScan!!.startCamera()
            } else {
                println("Camera permission not granted, requesting permission.")
                PermissionUtils.requestPermission(
                    this, Manifest.permission.CAMERA, requestCodeCamera
                )
            }
        }
    }

    /**
     * 释放相机
     */
    private fun releaseCamera() {
        if (mCameraScan != null) mCameraScan!!.release()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeCamera)
            requestCameraPermissionResult(permissions, grantResults)
    }

    /**
     * 请求Camera权限回调结果
     *
     * @param permissions  权限
     * @param grantResults 授权结果
     */
    fun requestCameraPermissionResult(permissions: Array<String>, grantResults: IntArray) {
        if (PermissionUtils.requestPermissionsResult(
                Manifest.permission.CAMERA, permissions, grantResults
            )
        ) startCamera()
        else finish()
    }


    override fun onScanResultCallback(result: AnalyzeResult<MutableList<DetectedObject>>?) {
        mCameraScan!!.setAnalyzeImage(true)
        if (result == null){
            objectBoundsView!!.setObjectBounds(null)
            return
        }
        objectBoundsView!!.setObjectBounds(result.result[0].boundingBox)
        if (!showDialog) return
        val bitmap = result.bitmap?.drawRect { canvas, paint ->
            for (data in result.result) {
                canvas.drawRect(data.boundingBox, paint)
            }
        }

        val config = AppDialogConfig(this, R.layout.result_dialog)
        config.setOnClickConfirm {
            AppDialog.INSTANCE.dismissDialog()
            mCameraScan!!.setAnalyzeImage(true)
        }.setOnClickCancel {
            AppDialog.INSTANCE.dismissDialog()
//            finish()
        }
        val imageView = config.getView<ImageView>(R.id.ivDialogContent)
        imageView.setImageBitmap(bitmap)
        AppDialog.INSTANCE.showDialog(config, false)
    }

    fun createAnalyzer(): Analyzer<MutableList<DetectedObject>?>? {
        return ObjectDetectionAnalyzer()
    }
}