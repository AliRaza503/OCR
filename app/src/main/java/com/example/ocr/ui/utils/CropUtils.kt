package com.example.ocr.ui.utils

import android.graphics.PointF
import kotlin.math.pow
import kotlin.math.sqrt


object CropUtils {
    fun getPointsDistance(p1: PointF, p2: PointF): Double {
        return getPointsDistance(p1.x, p1.y, p2.x, p2.y)
    }

    fun getPointsDistance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        return sqrt((x1 - x2).toDouble().pow(2.0) + (y1 - y2).toDouble().pow(2.0))
    }
}