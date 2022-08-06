package com.example.ocr.ui.utils.views

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.example.ocr.R

class RoundCornerPoints @JvmOverloads constructor(
    context: Context,
    private val rectangleView: RectangleView? = null,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private var downPoint = PointF()
    private var startPoint = PointF()
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        if (rectangleView != null) {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPoint.x, event.y - downPoint.y)
                    if (startPoint.x + mv.x + width < rectangleView.width &&
                        startPoint.y + mv.y + height < rectangleView.height &&
                        startPoint.x + mv.x > 0 && startPoint.y + mv.y > 0
                    ) {
                        x = startPoint.x + mv.x
                        y = startPoint.y + mv.y
                        startPoint = PointF(x, y)
                        rectangleView.zooming = true
                        rectangleView.zoomPos = startPoint
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    downPoint.x = event.x
                    downPoint.y = event.y
                    startPoint = PointF(x, y)
                    rectangleView.zooming = true
                    rectangleView.zoomPos = startPoint
                }
                MotionEvent.ACTION_UP -> {
                    performClick()
                    rectangleView.zooming = false
                }
            }
            rectangleView.invalidate()
        }
        return true
    }


    // Because we call this from onTouchEvent, this code will be executed for both
    // normal touch events and for when the system calls this using Accessibility
    override fun performClick(): Boolean {
        super.performClick()

        rectangleView?.paint?.color =
            if (rectangleView?.isValidShape(rectangleView.getPoints()) == true) {
                ContextCompat.getColor(context, R.color.teal_700)
            } else {
                ContextCompat.getColor(context, R.color.red)
            }

        return true
    }
}