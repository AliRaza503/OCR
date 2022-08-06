package com.example.ocr.ui.utils

import android.graphics.Point
import kotlin.math.pow
import kotlin.math.sqrt


object CropUtils {
    fun getPointsDistance(p1: Point, p2: Point): Double {
        return getPointsDistance(p1.x.toFloat(), p1.y.toFloat(), p2.x.toFloat(), p2.y.toFloat())
    }

    fun getPointsDistance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        return sqrt((x1 - x2).toDouble().pow(2.0) + (y1 - y2).toDouble().pow(2.0))
    }
}