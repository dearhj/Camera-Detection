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
    private var scaledFaceRect: Rect? = null
    private var rectPaint: Paint? = null
    private var scaleFactorTop = 0f
    private var scaleFactorBottom = 0f
    private var scaleFactorLeft = 0f
    private var scaleFactorRight = 0f
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var verticalGap = 0

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

        println("这里的值是》》   $screenWidth   $screenHeight ${(CAMERA_HEIGHT / CAMERA_WIDTH.toFloat())}  $verticalGap")
    }

    fun setFaceBounds(objectBounds: Rect) {
        scaleFactorLeft = objectBounds.left / CAMERA_WIDTH.toFloat()
        scaleFactorRight = objectBounds.right / CAMERA_WIDTH.toFloat()
        scaleFactorTop = objectBounds.top / CAMERA_HEIGHT.toFloat()
        scaleFactorBottom = objectBounds.bottom / CAMERA_HEIGHT.toFloat()
        println("这里的值是 缩放比》》   $scaleFactorLeft   $scaleFactorRight   $scaleFactorTop    $scaleFactorBottom")
        scaledFaceRect = Rect()
        scaledFaceRect!!.left = (screenWidth * scaleFactorLeft).toInt()
        scaledFaceRect!!.top = (screenHeight * scaleFactorTop).toInt()
        scaledFaceRect!!.right = (screenWidth * scaleFactorRight).toInt()
        scaledFaceRect!!.bottom = (screenHeight * scaleFactorBottom).toInt()
        if (scaledFaceRect!!.top < verticalGap) scaledFaceRect!!.top = verticalGap
        if (scaledFaceRect!!.bottom > (screenHeight - verticalGap))
            scaledFaceRect!!.bottom = (screenHeight - verticalGap).toInt()
        println("这里的值是 缩放后显示大小 》》   ${scaledFaceRect!!.left}   ${scaledFaceRect!!.top}   ${scaledFaceRect!!.right}    ${scaledFaceRect!!.bottom}")

        this.objectBound = scaledFaceRect
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (objectBound != null) canvas.drawRect(objectBound!!, rectPaint!!)
    }
}