package com.macan.guestbookkemendagri.captureframecomponent

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout


class BoundingBoxFrameLayout : FrameLayout {
    var paint: Paint

    companion object{
        var left: Float = 0F
        var top: Float = 0F
        var right: Float = 0F
        var bottom: Float = 0F

    }


    constructor(context: Context?) : super(context!!) {
        // TODO Auto-generated constructor stub
        setWillNotDraw(false)
        paint = Paint()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        // TODO Auto-generated constructor stub
        setWillNotDraw(false)
        paint = Paint()
    }

    override fun onDraw(canvas: Canvas) {
        /*
        Paint fillPaint = paint;
        fillPaint.setARGB(255, 0, 255, 0);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(fillPaint) ;
        */
        val boxPaint = Paint()
        boxPaint.color = Color.GREEN
        boxPaint.style = Paint.Style.STROKE
//        boxPaint.setPathEffect(DashPathEffect(floatArrayOf(300f, 800f), 10000F))
        boxPaint.strokeWidth = 10.0f

        //kiri atas
        canvas.drawLine( (width * 0.2).toFloat(),
                     (height * 0.2).toFloat(),
                     (width * 0.2).toFloat() + (width * 0.1).toFloat(),
                     (height * 0.2).toFloat(),
            boxPaint
            )

        canvas.drawLine( (width * 0.2).toFloat(),
                     (height * 0.2).toFloat(),
                     (width * 0.2).toFloat(),
                     (height * 0.2).toFloat() + (width * 0.1).toFloat(),
            boxPaint
            )
        //kanan atas
        canvas.drawLine( (width * 0.8).toFloat(),
                        (height * 0.2).toFloat(),
                  (width * 0.8).toFloat() - (width * 0.1).toFloat(),
            (height * 0.2).toFloat(),
            boxPaint
        )

        canvas.drawLine( (width * 0.8).toFloat(),
            (height * 0.2).toFloat(),
            (width * 0.8).toFloat(),
            (height * 0.2).toFloat() + (width * 0.1).toFloat(),
            boxPaint
        )

        //kiri bawah
        canvas.drawLine( (width * 0.2).toFloat(),
            (height * 0.8).toFloat() - (width * 0.1).toFloat(),
            (width * 0.2).toFloat(),
            (height * 0.8).toFloat(),
            boxPaint
        )

        canvas.drawLine( (width * 0.2).toFloat(),
            (height * 0.8).toFloat(),
            (width * 0.2).toFloat() + (width * 0.1).toFloat(),
            (height * 0.8).toFloat() ,
            boxPaint
        )


        //kanan bawah
        canvas.drawLine( (width * 0.8).toFloat() - (width * 0.1).toFloat(),
            (height * 0.8).toFloat(),
            (width * 0.8).toFloat(),
            (height * 0.8).toFloat(),
            boxPaint
        )

        canvas.drawLine( (width * 0.8).toFloat(),
            (height * 0.8).toFloat(),
            (width * 0.8).toFloat(),
            (height * 0.8).toFloat() - (width * 0.1).toFloat(),
            boxPaint
        )


        BoundingBoxFrameLayout.left = (width * 0.2).toFloat()
        BoundingBoxFrameLayout.top = (height * 0.2).toFloat()
        BoundingBoxFrameLayout.right = (width * 0.8).toFloat()
        BoundingBoxFrameLayout.bottom = (height * 0.8).toFloat()
//
//
//
//        canvas.drawRect(BoundingBoxFrameLayout.left, BoundingBoxFrameLayout.top, BoundingBoxFrameLayout.right, BoundingBoxFrameLayout.bottom, boxPaint)
    }
}