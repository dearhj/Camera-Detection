package com.android.detection.detection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.apply

class SquareOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private var horizontalSpacing = 50
    private var longitudinalSpacing = 210

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val topLeftX = horizontalSpacing
        val topLeftY = longitudinalSpacing
        val bottomLeftX = width - horizontalSpacing
        val bottomLeftY = height - longitudinalSpacing

        canvas.drawRect(topLeftX.toFloat(), topLeftY.toFloat(), bottomLeftX.toFloat(), bottomLeftY.toFloat(), paint)
    }
}