package com.example.ocr.ui.utils

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

internal object PerspectiveTransformation {
    fun transform(src: Mat, corners: MatOfPoint2f): Mat {
        val size = getRectangleSize(corners)
        val result = Mat.zeros(size, src.type())
        val imageOutline = getOutline(result)
        val transformation = Imgproc.getPerspectiveTransform(corners, imageOutline)
        Imgproc.warpPerspective(src, result, transformation, size)
        return result
    }

    private fun getDistance(p1: Point, p2: Point): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun getRectangleSize(rectangle: MatOfPoint2f): Size {
        val corners = rectangle.toArray()
        val top = getDistance(corners[0], corners[1])
        val right = getDistance(corners[1], corners[2])
        val bottom = getDistance(corners[2], corners[3])
        val left = getDistance(corners[3], corners[0])
        val averageWidth = (top + bottom) / 2f
        val averageHeight = (right + left) / 2f
        return Size(Point(averageWidth, averageHeight))
    }

    private fun getOutline(image: Mat): MatOfPoint2f {
        val topLeft = Point(0.toDouble(), 0.toDouble())
        val topRight = Point(image.cols().toDouble(), 0.toDouble())
        val bottomRight = Point(image.cols().toDouble(), image.rows().toDouble())
        val bottomLeft = Point(0.toDouble(), image.rows().toDouble())
        val points = arrayOf(topLeft, topRight, bottomRight, bottomLeft)
        val result = MatOfPoint2f()
        result.fromArray(*points)
        return result
    }
}