package com.android.detection.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

fun Bitmap.drawRect(block: (canvas: Canvas, paint: Paint) -> Unit): Bitmap {
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(result)
        canvas.drawBitmap(this, 0f, 0f, null)
        val paint = Paint()
        paint.strokeWidth = 6f
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED

        block(canvas, paint)

        canvas.save()
        canvas.restore()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}

/**
 * 获取[Bitmap]
 */
fun Context.getBitmap(uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(contentResolver, uri)
        ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
    } else {
        MediaStore.Images.Media.getBitmap(contentResolver, uri)
    }
}

