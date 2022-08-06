package com.example.ocr.ui.utils.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.ocr.R


class RectangleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    //paints
    var paint: Paint = Paint()
    private var shaderPaint = Paint()
    private var zoomCirclePaint = Paint()

    //Pointers image view
    private var pointer1: ImageView
    private var pointer2: ImageView
    private var pointer3: ImageView
    private var pointer4: ImageView

    /** Crop points:
     * 0->TopLeft, 1->TopRight， 2->BottomLeft, 3->BottomRight
     */
    var cropPoints =
        mutableListOf<PointF>()

    //Bitmap attrs
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    var zooming = false
    var zoomPos = PointF()

    private lateinit var bitmap: Bitmap

    companion object {
        private const val HALF = 2
        private const val THREE_PARTS = 3
    }

    init {
        pointer1 = getImageView(0, 0)
        pointer2 = getImageView(width, 0)
        pointer3 = getImageView(0, height)
        pointer4 = getImageView(width, height)

        addView(pointer1)
        addView(pointer2)
        addView(pointer3)
        addView(pointer4)

        paint.color = ContextCompat.getColor(context, R.color.teal_700)
        paint.strokeWidth = context.resources.getDimension(R.dimen.rectangle_line_stroke_width)
        paint.isAntiAlias = true

        //Shader paint init
        shaderPaint.apply {
            isAntiAlias = true
        }
        zoomCirclePaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(context, R.color.blue)
            strokeWidth = context.resources.getDimension((R.dimen.rectangle_line_stroke_width))
        }

    }

    fun getOrderedValidEdgePoints(tempBitmap: Bitmap, pointFs: List<PointF>): Map<Int, PointF> {
        var orderedPoints: Map<Int, PointF> = getOrderedPoints(pointFs)
        if (!isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap)
        }
        bitmap = tempBitmap
        shaderPaint.shader = BitmapShader(tempBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        bitmapWidth = tempBitmap.width
        bitmapHeight = tempBitmap.height
        return orderedPoints
    }

    fun setPoints(pointFMap: Map<Int, PointF>) {
        if (pointFMap.size == 4) {
            setPointsCoordinates(pointFMap)
        }
    }

    fun getPoints(): Map<Int, PointF> {
        val points: MutableList<PointF> = ArrayList()
        points.add(PointF(pointer1.x, pointer1.y))
        points.add(PointF(pointer2.x, pointer2.y))
        points.add(PointF(pointer3.x, pointer3.y))
        points.add(PointF(pointer4.x, pointer4.y))
        return getOrderedPoints(points)
    }

    //TODO: valid shape is not detecting a right shape
    fun isValidShape(pointFMap: Map<Int, PointF>): Boolean {
        return pointFMap.size == 4
    }

    private fun getOutlinePoints(tempBitmap: Bitmap): Map<Int, PointF> {
        val offsetWidth = (tempBitmap.width / THREE_PARTS).toFloat()
        val offsetHeight = (tempBitmap.height / THREE_PARTS).toFloat()
        val screenXCenter = tempBitmap.width / HALF
        val screenYCenter = tempBitmap.height / HALF
        val outlinePoints: MutableMap<Int, PointF> = HashMap()
        outlinePoints[0] = PointF(screenXCenter - offsetWidth, screenYCenter - offsetHeight)
        outlinePoints[1] = PointF(screenXCenter + offsetWidth, screenYCenter - offsetHeight)
        outlinePoints[2] = PointF(screenXCenter - offsetWidth, screenYCenter + offsetHeight)
        outlinePoints[3] = PointF(screenXCenter + offsetWidth, screenYCenter + offsetHeight)
        return outlinePoints
    }

    private fun getOrderedPoints(points: List<PointF>): Map<Int, PointF> {
        val centerPoint = PointF()
        val size = points.size
        for (pointF in points) {
            centerPoint.x += pointF.x / size
            centerPoint.y += pointF.y / size
        }
        val orderedPoints: MutableMap<Int, PointF> = HashMap()
        for (pointF in points) {
            var index = -1
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3
            }
            orderedPoints[index] = pointF
        }
        return orderedPoints
    }

    private fun setPointsCoordinates(pointFMap: Map<Int, PointF>) {
        pointer1.x = pointFMap.getValue(0).x
        pointer1.y = pointFMap.getValue(0).y

        pointer2.x = pointFMap.getValue(1).x
        pointer2.y = pointFMap.getValue(1).y

        pointer3.x = pointFMap.getValue(2).x
        pointer3.y = pointFMap.getValue(2).y

        pointer4.x = pointFMap.getValue(3).x
        pointer4.y = pointFMap.getValue(3).y
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (zooming) {
            shaderPaint.shader =
                BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val zoomMatrix = Matrix()
            val widthRatio = bitmapWidth / canvas.width.toFloat() // This can be omitted if 1.0
            val heightRatio = bitmapHeight / canvas.height.toFloat() // This can be omitted if 1.0
            zoomMatrix.reset()
            zoomMatrix.postScale(
                4f,
                4f,
                zoomPos.x * widthRatio,
                zoomPos.y * heightRatio
            )
            shaderPaint.shader.setLocalMatrix(zoomMatrix)
            //TODO: instead of zoomPos use the accurate corner point of the selected edge
            //TODO: draw the circle below or above the touch position
            canvas.drawCircle(zoomPos.x, zoomPos.y, 150f, shaderPaint)
            canvas.drawCircle(zoomPos.x, zoomPos.y, 150f, zoomCirclePaint)
        }
        canvas.drawLine(
            pointer1.x + pointer1.width / 2, pointer1.y + pointer1.height / 2,
            pointer3.x + pointer3.width / 2, pointer3.y + pointer3.height / 2,
            paint
        )
        canvas.drawLine(
            pointer1.x + pointer1.width / 2, pointer1.y + pointer1.height / 2,
            pointer2.x + pointer2.width / 2, pointer2.y + pointer2.height / 2,
            paint
        )
        canvas.drawLine(
            pointer2.x + pointer2.width / 2, pointer2.y + pointer2.height / 2,
            pointer4.x + pointer4.width / 2, pointer4.y + pointer4.height / 2,
            paint
        )
        canvas.drawLine(
            pointer3.x + pointer3.width / 2, pointer3.y + pointer3.height / 2,
            pointer4.x + pointer4.width / 2, pointer4.y + pointer4.height / 2,
            paint
        )

    }

    private fun getImageView(x: Int, y: Int): ImageView {
        val imageView = RoundCornerPoints(context, this)
        val layoutParams =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.layoutParams = layoutParams
        imageView.setImageResource(R.drawable.crop_corner_circle)
        imageView.x = x.toFloat()
        imageView.y = y.toFloat()
        return imageView
    }
}