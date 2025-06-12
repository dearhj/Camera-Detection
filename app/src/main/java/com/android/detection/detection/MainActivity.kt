package com.android.detection.detection

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.android.detection.detection.analyze.Analyzer
import com.android.detection.detection.analyze.ObjectDetectionAnalyzer
import com.android.detection.detection.util.PermissionUtils
import com.google.mlkit.vision.objects.DetectedObject
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig


@RequiresApi(Build.VERSION_CODES.R)
class MainActivity : AppCompatActivity(),
    CameraScan.OnScanResultCallback<MutableList<DetectedObject>> {

    private val requestCodeCamera: Int = 0x86
//    private var previewView: PreviewView? = null
    private var previewView: TextureView? = null
    private var mCameraScan: CameraScan<MutableList<DetectedObject>>? = null
    private var objectBoundsView: ObjectBoundsView? = null
    private var tack: Button? = null
    private var picture: Button? = null
    private var flash: Button? = null
    private var flashStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera2_scan)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.isNavigationBarContrastEnforced = false
        tack = findViewById(R.id.take)
        picture = findViewById(R.id.picture)
        flash = findViewById(R.id.flash)
        tack?.visibility = View.GONE
        objectBoundsView = findViewById(R.id.object_bound)
//        previewView = findViewById<PreviewView?>(R.id.previewView)
        previewView = findViewById<TextureView?>(R.id.previewView)
//        mCameraScan = BaseCameraScan(this, previewView!!)
        mCameraScan = BaseCamera2Scan<MutableList<DetectedObject>>(this, this, previewView!!)
        mCameraScan!!.setAnalyzer(createAnalyzer()).setOnScanResultCallback(this)
        flash?.setOnClickListener {
            if (!flashStatus) {
                flashStatus = true
                mCameraScan?.enableTorch(true)
                flash?.text = "闪光灯开"
            } else {
                flashStatus = false
                mCameraScan?.enableTorch(false)
                flash?.text = "闪光灯关"
            }
        }
        tack?.setOnClickListener { mCameraScan?.takePhoto() }
        picture?.setOnClickListener {
            try {
                Intent(
                    Intent.ACTION_VIEW,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ).apply { startActivity(this) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        requestFilePermission()
    }

    override fun onResume() {
        startCamera()
//        flashStatus = false
//        flash?.text = "闪光灯关"
        super.onResume()
    }


    override fun onDestroy() {
        releaseCamera()
        super.onDestroy()
    }

    fun requestFilePermission() {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = "package:${applicationContext.packageName}".toUri()
            startActivityForResult(intent, 2)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            if (Environment.isExternalStorageManager()) {
            } else Toast.makeText(this, "文件读写权限被拒绝，功能将受限", Toast.LENGTH_SHORT).show()
        }
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
        if (result == null) {
            objectBoundsView!!.setObjectBounds(null) {}
            tack?.visibility = View.GONE
            return
        }
        objectBoundsView!!.setObjectBounds(result.result[0].boundingBox) {
            if (it) tack?.visibility = View.VISIBLE
            else tack?.visibility = View.GONE
        }
//        val bitmap = result.bitmap?.drawRect { canvas, paint ->
//            for (data in result.result) {
//                canvas.drawRect(data.boundingBox, paint)
//            }
//        }
//
//        val config = AppDialogConfig(this, R.layout.result_dialog)
//        config.setOnClickConfirm {
//            AppDialog.INSTANCE.dismissDialog()
//            mCameraScan!!.setAnalyzeImage(true)
//        }.setOnClickCancel {
//            AppDialog.INSTANCE.dismissDialog()
////            finish()
//        }
//        val imageView = config.getView<ImageView>(R.id.ivDialogContent)
//        imageView.setImageBitmap(bitmap)
//        AppDialog.INSTANCE.showDialog(config, false)
    }

    fun createAnalyzer(): Analyzer<MutableList<DetectedObject>?>? {
        return ObjectDetectionAnalyzer()
    }
}