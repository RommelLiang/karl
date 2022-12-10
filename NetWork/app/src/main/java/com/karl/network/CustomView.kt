package com.karl.network

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CustomView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val mPaint by lazy{
        Paint().apply {
           style = Paint.Style.FILL
           color = Color.RED
        }
    }
    override fun onDraw(canvas: Canvas?) {
        canvas?.drawCircle(200f,200f,100f,mPaint)
        super.onDraw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }


}