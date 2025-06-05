package com.android.detection.detection


import android.widget.ImageView
import com.android.detection.camerascan.AnalyzeResult
import com.google.mlkit.vision.objects.DetectedObject
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig

class MainActivity : ObjectCameraScanActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            WindowCompat.setDecorFitsSystemWindows(window, false)
//            window.isNavigationBarContrastEnforced = false
//        }
//    }

    override fun onScanResultCallback(result: AnalyzeResult<MutableList<DetectedObject>>) {
//        cameraScan.setAnalyzeImage(false)
        println("这里收到了数据，，，，，， ")
        cameraScan.setAnalyzeImage(true)
        val bitmap = result.bitmap?.drawRect { canvas, paint ->
            for (data in result.result) {
                canvas.drawRect(data.boundingBox, paint)
            }
        }

        val config = AppDialogConfig(this, R.layout.result_dialog)
        config.setOnClickConfirm {
            AppDialog.INSTANCE.dismissDialog()
            cameraScan.setAnalyzeImage(true)
        }.setOnClickCancel {
            AppDialog.INSTANCE.dismissDialog()
            finish()
        }
        val imageView = config.getView<ImageView>(R.id.ivDialogContent)
        imageView.setImageBitmap(bitmap)
        AppDialog.INSTANCE.showDialog(config, false)
    }
}