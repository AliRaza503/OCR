package com.example.ocr.ui.utils.extensions

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar


internal fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.toMat(): Mat {
    val mat = Mat(this.height, this.width, CvType.CV_8U, Scalar(4.toDouble()))
    val bitmap32 = this.copy(Bitmap.Config.ARGB_8888, true)
    Utils.bitmapToMat(bitmap32, mat)
    return mat
}

internal fun Mat.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(this, bitmap)
    return bitmap
}

internal fun Bitmap.scaledBitmap(width: Int, height: Int): Bitmap {
    val m = Matrix()
    m.setRectToRect(
        RectF(0f, 0f, this.width.toFloat(), this.height.toFloat()),
        RectF(0f, 0f, width.toFloat(), height.toFloat()),
        Matrix.ScaleToFit.CENTER
    )
    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, m, true)
}