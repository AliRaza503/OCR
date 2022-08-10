package com.example.ocr.ui.utils.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.ocr.R
import com.example.ocr.ui.utils.CropUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

const val MASK_ALPHA = 100
const val TOUCH_POINT_CATCH_DISTANCE = 15f
const val P_LT = 0
const val P_RT = 1
const val P_RB = 2
const val P_LB = 3
const val MAGNIFIER_BORDER_WIDTH = 1f

//TODO: not proper magnification
class RectangleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    //Paints
    private val cornerPointPaint = Paint()
    private val linePaint = Paint()
    private val maskPaint = Paint()
    private val magnifierPaint = Paint()
    private val magnifierCrossPaint = Paint()

    private val maskXfermode: Xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    private var magnifierDrawable: ShapeDrawable? = null
    private val magnifierMatrix = Matrix()

    //Redrawn on each rotation
    var imgDrawn = false

    // where the image is actually displayed
    private var drawableWidth = 0f
    private var drawableHeight = 0f
    private var drawableLeft = 0f
    private var drawableTop = 0f

    //The scaling factor of image
    private var mScaleX = 0f
    private var mScaleY = 0f
    private val mMatrixValue = FloatArray(9)

    //Image touch handler helpers
    private var draggingPoint: PointF? = null

    internal enum class DragPointType {
        LEFT_TOP, RIGHT_TOP, RIGHT_BOTTOM, LEFT_BOTTOM;
    }

    //the actual image position
    private val drawablePosition: Unit
        get() {
            val drawable = drawable
            if (drawable != null) {
                imageMatrix.getValues(mMatrixValue)
                mScaleX = mMatrixValue[Matrix.MSCALE_X]
                mScaleY = mMatrixValue[Matrix.MSCALE_Y]
                drawableWidth = drawable.intrinsicWidth.toFloat() * mScaleX
                drawableHeight = drawable.intrinsicHeight.toFloat() * mScaleY
                drawableLeft = ((width - drawableWidth) / 2)
                drawableTop = ((height - drawableHeight) / 2)
            }
        }

    /** Crop points:
     * 0->TopLeft, 1->TopRight， 2->BottomRight, 3->BottomLeft
     */
    private var cropPoints = arrayListOf<PointF>()
    private val pointRadius = 30f
    private val pointToLinePath = Path()

    init {
        //initializing paints
        cornerPointPaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(context, R.color.blue)
            strokeWidth = resources.getDimension(R.dimen.rectangle_line_stroke_width)
        }
        linePaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(context, R.color.teal_700)
            strokeWidth = resources.getDimension(R.dimen.rectangle_line_stroke_width)
        }
        maskPaint.apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.FILL
            alpha = MASK_ALPHA
        }
        //Initializing magnifier paint
        magnifierPaint.apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        magnifierCrossPaint.apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.teal_700)
            style = Paint.Style.FILL
            strokeWidth = dp2px(1f)
        }
        //Adding 4 elements
        repeat(4) {
            cropPoints.add(PointF(0f, 0f))
        }
    }

    //Initialize the magnifier
    private fun initMagnifier() {
        val bitmap = this.drawable.toBitmap().copy(Bitmap.Config.RGB_565, true)
        val canvas = Canvas(bitmap)
//        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(
            bitmap,
            null,
            Rect(
                drawableLeft.toInt(),
                drawableTop.toInt(),
                (drawableWidth + drawableLeft).toInt(),
                (drawableHeight + drawableTop).toInt()
            ),
            null
        )
        canvas.save()
        val magnifierShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        magnifierDrawable = ShapeDrawable(OvalShape())
        magnifierDrawable!!.paint.shader = magnifierShader
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawablePosition
        //will only initialize the variables on image drawn
        fullImageCropPoint()
        drawPoints(canvas)
        drawMask(canvas)
        drawLines(canvas)
        drawMagnifier(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        var handle = true
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                draggingPoint = getNearbyPoint(event)
                if (draggingPoint == null)
                    handle = false
            }
            MotionEvent.ACTION_UP -> draggingPoint = null
            MotionEvent.ACTION_MOVE -> toImagePointSize(draggingPoint, event)
        }
        invalidate()
        return handle || super.onTouchEvent(event)
    }

    /**
     * Dragging point helper
     */
    private fun toImagePointSize(draggingPoint: PointF?, event: MotionEvent) {
        if (draggingPoint == null) return
        val pointType = getPointType(draggingPoint)
        val x = ((min(
            max(event.x, drawableLeft),
            (drawableLeft + drawableWidth)
        ) - drawableLeft) / mScaleX).toInt()
        val y = ((min(
            max(event.y, drawableTop),
            (drawableTop + drawableHeight)
        ) - drawableTop) / mScaleY).toInt()

        if (pointType != null) {
            when (pointType) {
                DragPointType.LEFT_TOP -> if (!canMoveLeftTop(x, y)) return
                DragPointType.RIGHT_TOP -> if (!canMoveRightTop(x, y)) return
                DragPointType.RIGHT_BOTTOM -> if (!canMoveRightBottom(x, y)) return
                DragPointType.LEFT_BOTTOM -> if (!canMoveLeftBottom(x, y)) return
            }
        }
        draggingPoint.x = x.toFloat()
        draggingPoint.y = y.toFloat()
    }

    private fun getPointType(dragPoint: PointF?): DragPointType? {
        if (dragPoint == null) return null
        val type: DragPointType
        for (i in cropPoints.indices) {
            if (dragPoint === cropPoints[i]) {
                type = DragPointType.values()[i]
                return type
            }
        }
        return null
    }

    /**
     * Movement helpers
     */
    private fun canMoveLeftTop(x: Int, y: Int): Boolean {
        if (pointSideLine(
                cropPoints[P_RT],
                cropPoints[P_LB], x, y
            )
            * pointSideLine(
                cropPoints[P_RT],
                cropPoints[P_LB], cropPoints[P_RB]
            ) > 0
        ) {
            return false
        }
        if (pointSideLine(
                cropPoints[P_RT],
                cropPoints[P_RB], x, y
            )
            * pointSideLine(
                cropPoints[P_RT],
                cropPoints[P_RB], cropPoints[P_LB]
            ) < 0
        ) {
            return false
        }
        return (pointSideLine(
            cropPoints[P_LB],
            cropPoints[P_RB], x, y
        )
                * pointSideLine(
            cropPoints[P_LB],
            cropPoints[P_RB], cropPoints[P_RT]
        )) >= 0
    }

    private fun canMoveRightTop(x: Int, y: Int): Boolean {
        if (pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_RB], x, y
            )
            * pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_RB], cropPoints[P_LB]
            ) > 0
        ) {
            return false
        }
        if (pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_LB], x, y
            )
            * pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_LB], cropPoints[P_RB]
            ) < 0
        ) {
            return false
        }
        return (pointSideLine(
            cropPoints[P_LB],
            cropPoints[P_RB], x, y
        )
                * pointSideLine(
            cropPoints[P_LB],
            cropPoints[P_RB], cropPoints[P_LT]
        )) >= 0
    }

    private fun canMoveRightBottom(x: Int, y: Int): Boolean {
        if (pointSideLine(
                cropPoints[P_RT],
                cropPoints[P_LB], x, y
            )
            * pointSideLine(
                cropPoints[P_RT],
                cropPoints[P_LB], cropPoints[P_LT]
            ) > 0
        ) {
            return false
        }
        if (pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_RT], x, y
            )
            * pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_RT], cropPoints[P_LB]
            ) < 0
        ) {
            return false
        }
        return (pointSideLine(
            cropPoints[P_LT],
            cropPoints[P_LB], x, y
        )
                * pointSideLine(
            cropPoints[P_LT],
            cropPoints[P_LB], cropPoints[P_RT]
        )) >= 0
    }

    private fun canMoveLeftBottom(x: Int, y: Int): Boolean {
        if (pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_RB], x, y
            )
            * pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_RB], cropPoints[P_RT]
            ) > 0
        ) {
            return false
        }
        if (pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_RT], x, y
            )
            * pointSideLine(
                cropPoints[P_LT],
                cropPoints[P_RT], cropPoints[P_RB]
            ) < 0
        ) {
            return false
        }
        return (pointSideLine(
            cropPoints[P_RT],
            cropPoints[P_RB], x, y
        )
                * pointSideLine(
            cropPoints[P_RT],
            cropPoints[P_RB], cropPoints[P_LT]
        )) >= 0
    }

    /**
     * Determining the line of the point touched
     */
    private fun pointSideLine(lineP1: PointF?, lineP2: PointF?, x: Int, y: Int): Long {
        val x1 = lineP1!!.x.toLong()
        val y1 = lineP1.y.toLong()
        val x2 = lineP2!!.x.toLong()
        val y2 = lineP2.y.toLong()
        return (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1)
    }

    private fun pointSideLine(lineP1: PointF?, lineP2: PointF?, point: PointF?): Long {
        return pointSideLine(lineP1, lineP2, point!!.x.toInt(), point.y.toInt())
    }

    /**
     * Determining the touch point
     */
    private fun getNearbyPoint(event: MotionEvent): PointF? {
        for (p in cropPoints)
            if (isTouchPoint(p, event)) return p
        return null
    }

    private fun isTouchPoint(p: PointF, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val px = getViewPointX(p)
        val py = getViewPointY(p)
        val distance =
            sqrt((x - px).toDouble().pow(2.0) + (y - py).toDouble().pow(2.0))
        return distance < dp2px(TOUCH_POINT_CATCH_DISTANCE)
    }

    private fun dp2px(dp: Float) = dp * resources.displayMetrics.density

    /**
     * Set to full image crop
     */
    private fun fullImageCropPoint() {
        try {
            if (!imgDrawn) {
                val bitmap = this.drawable.toBitmap()
                val width = bitmap.width
                val height = bitmap.height
                cropPoints[0] = PointF(0f, 0f)
                cropPoints[1] = PointF(width.toFloat(), 0f)
                cropPoints[2] = PointF(width.toFloat(), height.toFloat())
                cropPoints[3] = PointF(0f, height.toFloat())
                Log.d("crop points", "$cropPoints")
                imgDrawn = true
            }
        } catch (e: NullPointerException) {
            Log.d("$e", "The pointer drawable is null")
            return
        }
    }

    /**
     * Drawing helper methods
     */
    private fun drawPoints(canvas: Canvas) {
        for (point in cropPoints) {
            canvas.drawCircle(
                getViewPointX(point), getViewPointY(point), pointRadius,
                cornerPointPaint
            )
        }
    }

    private fun drawMask(canvas: Canvas) {
        val path = resetPointPath()
        //to make it src
        val sc = canvas.saveLayer(
            drawableLeft,
            drawableTop,
            drawableLeft + drawableWidth,
            drawableTop + drawableHeight,
            maskPaint
            //Save the flags here
        )
        maskPaint.alpha = MASK_ALPHA
        canvas.drawRect(
            drawableLeft,
            drawableTop,
            drawableLeft + drawableWidth,
            drawableTop + drawableHeight,
            maskPaint
        )
        //Clear the crop rectangle
        maskPaint.apply {
            alpha = 255
            xfermode = maskXfermode
        }
        canvas.drawPath(path, maskPaint)
        //Reset the xfermode for the next drawing
        maskPaint.xfermode = null
        canvas.restoreToCount(sc)
    }

    private fun drawLines(canvas: Canvas) {
        canvas.drawPath(resetPointPath(), linePaint)
    }

    private fun drawMagnifier(canvas: Canvas) {
        if (draggingPoint != null) {
            if (magnifierDrawable == null) {
                initMagnifier()
            }
            val draggingX = getViewPointX(draggingPoint!!)
            val draggingY = getViewPointY(draggingPoint!!)
            val radius = (width / 8f)
            var cx = radius     //The x-coordinate of the center of the circle
            val lineOffset = dp2px(MAGNIFIER_BORDER_WIDTH).toInt()
            magnifierDrawable!!.setBounds(
                lineOffset,
                lineOffset,
                radius.toInt() * 2 - lineOffset,
                radius.toInt() * 2 - lineOffset
            )
            //If magnifier drawn is intersecting with one of the points then move the magnifier
            val pointsDistance: Double = CropUtils.getPointsDistance(draggingX, draggingY, 0f, 0f)
            if (pointsDistance < radius * 2.5) {
                magnifierDrawable!!.setBounds(
                    width - radius.toInt() * 2 + lineOffset,
                    lineOffset,
                    width - lineOffset,
                    radius.toInt() * 2 - lineOffset
                )
                cx = width - radius
            }
            canvas.drawCircle(cx, radius, radius, magnifierPaint)
            magnifierMatrix.reset()
            magnifierMatrix.setTranslate(radius - draggingX, radius - draggingY)
//            magnifierMatrix.setScale(4f, 4f, radius - draggingX, radius - draggingY)
            magnifierDrawable!!.paint.shader.setLocalMatrix(magnifierMatrix)
            magnifierDrawable!!.draw(canvas)
            val crossLength = dp2px(5f)
            canvas.drawLine(
                cx, radius - crossLength, cx, radius + crossLength,
                magnifierCrossPaint
            )
            canvas.drawLine(
                cx - crossLength, radius, cx + crossLength, radius,
                magnifierCrossPaint
            )
        }
    }

    /**
     * path drawing helper
     */
    private fun resetPointPath(): Path {
        pointToLinePath.reset()
        val tl = cropPoints[0]
        val tr = cropPoints[1]
        val br = cropPoints[2]
        val bl = cropPoints[3]
        pointToLinePath.moveTo(getViewPointX(tl), getViewPointY(tl))
        pointToLinePath.lineTo(getViewPointX(tr), getViewPointY(tr))
        pointToLinePath.lineTo(getViewPointX(br), getViewPointY(br))
        pointToLinePath.lineTo(getViewPointX(bl), getViewPointY(bl))
        pointToLinePath.close()
        return pointToLinePath
    }

    /**
     * Points getters for accuracy
     */
    private fun getViewPointX(point: PointF): Float {
        return (point.x * mScaleX) + drawableLeft
    }

    private fun getViewPointY(point: PointF): Float {
        return (point.y * mScaleY) + drawableTop
    }

}