package com.android.detection.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import com.android.detection.detection.CameraConfig.CAMERA_HEIGHT
import com.android.detection.detection.CameraConfig.CAMERA_WIDTH
import kotlin.math.max
import kotlin.math.min

class ObjectBoundsView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var objectBound: Rect? = null
    private var scaledObjectRect: Rect? = null
    private var rectPaint: Paint? = null
    private var scaleFactorTop = 0f
    private var scaleFactorBottom = 0f
    private var scaleFactorLeft = 0f
    private var scaleFactorRight = 0f
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var verticalGap = 0
    private var previewHeight = 0

    init {
        init()
    }

    private fun init() {
        rectPaint = Paint()
        rectPaint!!.color = Color.GREEN
        rectPaint!!.style = Paint.Style.STROKE
        rectPaint!!.strokeWidth = 5f
        val defaultDisplay =
            (context.getSystemService("window") as WindowManager).defaultDisplay
        val displayMetrics = DisplayMetrics()
        defaultDisplay.getRealMetrics(displayMetrics)
        screenWidth =
            min(displayMetrics.widthPixels.toDouble(), displayMetrics.heightPixels.toDouble())
                .toFloat()
        screenHeight =
            max(displayMetrics.widthPixels.toDouble(), displayMetrics.heightPixels.toDouble())
                .toFloat()
        verticalGap =
            ((screenHeight - screenWidth * (CAMERA_HEIGHT / CAMERA_WIDTH.toFloat())) / 2).toInt()
        previewHeight = (screenHeight - (2 * verticalGap)).toInt()
    }

    fun setObjectBounds(objectBounds: Rect) {
        scaleFactorLeft = objectBounds.left / CAMERA_WIDTH.toFloat()
        scaleFactorRight = objectBounds.right / CAMERA_WIDTH.toFloat()
        scaleFactorTop = objectBounds.top / CAMERA_HEIGHT.toFloat()
        scaleFactorBottom = objectBounds.bottom / CAMERA_HEIGHT.toFloat()
        scaledObjectRect = Rect()
        scaledObjectRect!!.left = (screenWidth * scaleFactorLeft).toInt()
        scaledObjectRect!!.right = (screenWidth * scaleFactorRight).toInt()
        scaledObjectRect!!.top = (previewHeight * scaleFactorTop).toInt() + verticalGap
        scaledObjectRect!!.bottom = (previewHeight * scaleFactorBottom).toInt() + verticalGap
        if (scaledObjectRect!!.top < verticalGap) scaledObjectRect!!.top = verticalGap
        if (scaledObjectRect!!.bottom > (screenHeight - verticalGap))
            scaledObjectRect!!.bottom = (screenHeight - verticalGap).toInt()

        this.objectBound = scaledObjectRect
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (objectBound != null) canvas.drawRect(objectBound!!, rectPaint!!)
    }
}