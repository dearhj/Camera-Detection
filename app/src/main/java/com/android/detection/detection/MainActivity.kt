package com.android.detection.detection


import android.widget.ImageView
import com.android.detection.detection.analyze.Analyzer
import com.android.detection.detection.analyze.ObjectDetectionAnalyzer
import com.google.mlkit.vision.objects.DetectedObject
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig

class MainActivity : BaseCameraScanActivity<MutableList<DetectedObject>>() {

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

    override fun createAnalyzer(): Analyzer<MutableList<DetectedObject>?>? {
        return ObjectDetectionAnalyzer()
    }
}