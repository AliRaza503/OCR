package com.example.ocr.ui.utils.views


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.example.ocr.R
import com.example.ocr.ui.utils.CropUtils
import kotlin.math.*

class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    private var mPointPaint: Paint? = null
    private var mPointFillPaint: Paint? = null
    private var mLinePaint: Paint? = null
    private var mMaskPaint: Paint? = null
    private var mMagnifierPaint: Paint? = null
    private var mMagnifierCrossPaint: Paint? = null
    private var mScaleX = 0f
    private var mScaleY // The zoom ratio of the displayed image to the actual image
            = 0f
    private var mActWidth = 0
    private var mActHeight = 0
    private var mActLeft = 0
    private var mActTop // where the image is actually displayed
            = 0
    private var mDraggingPoint: Point? = null
    private val mDensity: Float
    private var mMagnifierDrawable: ShapeDrawable? = null
    private val mMatrixValue = FloatArray(9)
    private val mMaskXfermode: Xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    private val mPointLinePath = Path()
    private val mMagnifierMatrix = Matrix()
    private var mCropPoints // Crop area, 0->LeftTop, 1->RightTop， 2->RightBottom, 3->LeftBottom
            : Array<Point?> = arrayOf()
    private var mEdgeMidPoints //edge midpoint
            : Array<Point?> = arrayOf()
    private var mLineWidth = 0f // the width of the selection line
    private var mPointColor = 0 //Anchor color
    private var mPointWidth = 0f //Anchor width

    var mPointFillAlpha = DEFAULT_POINT_FILL_ALPHA // 锚点填充颜色透明度
    var mLineColor = DEFAULT_LINE_COLOR // 选区线的颜色
    var mMagnifierCrossColor = DEFAULT_MAGNIFIER_CROSS_COLOR // 放大镜十字颜色
    var mMaskAlpha = DEFAULT_MASK_ALPHA //0 - 255, 蒙版透明度
    private var mShowMagnifier = true // 是否显示放大镜
    private var mShowEdgeMidPoint = true //是否显示边中点
    var mDragLimit = true // 是否限制锚点拖动范围为凸四边形
    var imgDrawn = false

    internal enum class DragPointType {
        LEFT_TOP, RIGHT_TOP, RIGHT_BOTTOM, LEFT_BOTTOM, TOP, RIGHT, BOTTOM, LEFT;

        companion object {
            fun isEdgePoint(type: DragPointType?): Boolean {
                return type == TOP || type == RIGHT || type == BOTTOM || type == LEFT
            }
        }
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        mMaskAlpha = 40 //Setting the mask alpha
        mLineColor = ContextCompat.getColor(context, R.color.teal_700)
        mLineWidth = resources.getDimension(R.dimen.rectangle_line_stroke_width)
        mPointColor = ContextCompat.getColor(context, R.color.blue)
        mPointWidth = DEFAULT_LINE_WIDTH
        mMagnifierCrossColor = ContextCompat.getColor(context, R.color.teal_700)
        mShowMagnifier = true

    }

    private fun setEdgeMidPoints() {
        val len = mCropPoints.size
        for (i in 0 until len) {
            mEdgeMidPoints[i]!![mCropPoints[i]!!.x + (mCropPoints[(i + 1) % len]!!.x - mCropPoints[i]!!.x) / 2] =
                mCropPoints[i]!!.y + (mCropPoints[(i + 1) % len]!!.y - mCropPoints[i]!!.y) / 2
        }
    }

    /**
     * 设置选区为包裹全图
     */
    private fun setFullImgCrop() {
        if (drawable == null) {
            Log.w(TAG, "should call after set drawable")
            return
        }
        mCropPoints = fullImgCropPoints
        imgDrawn = true
        invalidate()
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        mMagnifierDrawable = null
    }


    /**
     * 获取选区
     * @return 选区顶点
     */
    /**
     * 设置选区
     * @param cropPoints 选区顶点
     */
    var cropPoints: Array<Point?>?
        get() = mCropPoints
        set(cropPoints) {
            if (drawable == null) {
                Log.w(TAG, "should call after set drawable")
                return
            }
            if (!checkPoints(cropPoints)) {
                setFullImgCrop()
            } else {
                if (cropPoints != null) {
                    mCropPoints = cropPoints
                }
                invalidate()
            }
        }

    /**
     * 蒙版透明度
     * @param maskAlpha 透明度
     */
    fun setMaskAlpha(maskAlpha: Int) {
        var masksAlpha = maskAlpha
        masksAlpha = min(max(0, maskAlpha), 255)
        mMaskAlpha = masksAlpha
        invalidate()
    }


    /**
     * 设置选区线的颜色
     * @param lineColor 颜色
     */
    fun setLineColor(lineColor: Int) {
        mLineColor = lineColor
        invalidate()
    }

    /**
     *
    Set the color of the cross in magnifying glass
     * @param magnifierCrossColor color resource value
     */
    fun setMagnifierCrossColor(magnifierCrossColor: Int) {
        mMagnifierCrossColor = magnifierCrossColor
    }

    /**
     *Set selection line width
     * @param lineWidth width in px
     */
    fun setLineWidth(lineWidth: Int) {
        mLineWidth = lineWidth.toFloat()
        invalidate()
    }

    fun setPointColor(pointColor: Int) {
        mPointColor = pointColor
        invalidate()
    }

    fun setPointWidth(pointWidth: Float) {
        mPointWidth = pointWidth
        invalidate()
    }

    /**
     * Set whether to display the magnifying glass
     * @param showMagnifier
     */
    fun setShowMagnifier(showMagnifier: Boolean) {
        mShowMagnifier = showMagnifier
    }

    /**
     * Sets whether to restrict dragging to convex quads
     * @param dragLimit
     */
    fun setDragLimit(dragLimit: Boolean) {
        mDragLimit = dragLimit
    }

    /**
     * Whether selection is a convex quadrilateral
     * @return true：Convex Quadrilateral
     */
    fun canRightCrop(): Boolean {
        if (!checkPoints(mCropPoints)) {
            return false
        }
        val lt = mCropPoints[0]
        val rt = mCropPoints[1]
        val rb = mCropPoints[2]
        val lb = mCropPoints[3]
        return pointSideLine(lt, rb, lb) * pointSideLine(lt, rb, rt) < 0 && pointSideLine(
            lb,
            rt,
            lt
        ) * pointSideLine(lb, rt, rb) < 0
    }

    private fun checkPoints(points: Array<Point?>?): Boolean {
        return points != null && points.size == 4 && points[0] != null && points[1] != null && points[2] != null && points[3] != null
    }

    private fun pointSideLine(lineP1: Point?, lineP2: Point?, point: Point?): Long {
        return pointSideLine(lineP1, lineP2, point!!.x, point.y)
    }

    private fun pointSideLine(lineP1: Point?, lineP2: Point?, x: Int, y: Int): Long {
        val x1 = lineP1!!.x.toLong()
        val y1 = lineP1.y.toLong()
        val x2 = lineP2!!.x.toLong()
        val y2 = lineP2.y.toLong()
        return (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1)
    }

    val bitmap: Bitmap?
        get() {
            var bmp: Bitmap? = null
            val drawable = drawable
            if (drawable is BitmapDrawable) {
                bmp = drawable.bitmap
            }
            return bmp
        }

    private fun initPaints() {
        mPointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPointPaint!!.color = mPointColor
        mPointPaint!!.strokeWidth = mPointWidth
        mPointPaint!!.style = Paint.Style.STROKE
        mPointFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPointFillPaint!!.style = Paint.Style.FILL
        mPointFillPaint!!.alpha = mPointFillAlpha
        mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLinePaint!!.color = mLineColor
        mLinePaint!!.strokeWidth = mLineWidth
        mLinePaint!!.style = Paint.Style.STROKE
        mMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mMaskPaint!!.color = Color.BLACK
        mMaskPaint!!.style = Paint.Style.FILL
        mMagnifierPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mMagnifierPaint!!.color = Color.WHITE
        mMagnifierPaint!!.style = Paint.Style.FILL
        mMagnifierCrossPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mMagnifierCrossPaint!!.color = mMagnifierCrossColor
        mMagnifierCrossPaint!!.style = Paint.Style.FILL
        mMagnifierCrossPaint!!.strokeWidth = dp2px(MAGNIFIER_CROSS_LINE_WIDTH)
    }

    private fun initMagnifier() {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(
            bitmap,
            null,
            Rect(mActLeft, mActTop, mActWidth + mActLeft, mActHeight + mActTop),
            null
        )
        canvas.save()
        val magnifierShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        mMagnifierDrawable = ShapeDrawable(OvalShape())
        mMagnifierDrawable!!.paint.shader = magnifierShader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //Initialize image location information
        drawablePosition
        //init the crop points on first draw
        if (!imgDrawn) {
            cropPoints
        }
        //开始绘制选区
        onDrawCropPoint(canvas)
    }

    private fun onDrawCropPoint(canvas: Canvas) {
        //绘制蒙版
        onDrawMask(canvas)
        //绘制选区线
        onDrawLines(canvas)
        //绘制锚点
        onDrawPoints(canvas)
        //绘制放大镜
        onDrawMagnifier(canvas)
        //        onDrawCusMagnifier(canvas);
    }

    private fun onDrawCusMagnifier(canvas: Canvas) {
        val pointType = getPointType(mDraggingPoint)
        if (pointType == null || DragPointType.isEdgePoint(pointType)) {
            return
        }
        if (mShowMagnifier && mDraggingPoint != null) {
            if (mMagnifierDrawable == null) {
                initMagnifier()
            }
            val draggingX = getViewPointX(mDraggingPoint)
            val draggingY = getViewPointY(mDraggingPoint)
            val radius = (width / 8).toFloat()
            var cx = radius //圆心x坐标
            val lineOffset = dp2px(MAGNIFIER_BORDER_WIDTH).toInt()
            if (0 <= mDraggingPoint!!.x && mDraggingPoint!!.x < drawable.intrinsicWidth / 2) { //拉伸点在左侧时，放大镜显示在右侧
                mMagnifierDrawable!!.setBounds(
                    width - radius.toInt() * 2 + lineOffset,
                    lineOffset,
                    width - lineOffset,
                    radius.toInt() * 2 - lineOffset
                )
                cx = width - radius
            } else {
                mMagnifierDrawable!!.setBounds(
                    lineOffset,
                    lineOffset,
                    radius.toInt() * 2 - lineOffset,
                    radius.toInt() * 2 - lineOffset
                )
            }
            canvas.drawCircle(cx, radius, radius, mMagnifierPaint!!)
            mMagnifierMatrix.setTranslate(radius - draggingX, radius - draggingY)
            mMagnifierDrawable!!.paint.shader.setLocalMatrix(mMagnifierMatrix)
            mMagnifierDrawable!!.draw(canvas)
            //放大镜锚点
            canvas.drawCircle(
                cx, radius, dp2px(POINT_RADIUS),
                mPointFillPaint!!
            )
            canvas.drawCircle(
                cx, radius, dp2px(POINT_RADIUS),
                mPointPaint!!
            )
        }
    }

    private fun onDrawMagnifier(canvas: Canvas) {
        if (mShowMagnifier && mDraggingPoint != null) {
            if (mMagnifierDrawable == null) {
                initMagnifier()
            }
            val draggingX = getViewPointX(mDraggingPoint)
            val draggingY = getViewPointY(mDraggingPoint)
            val radius = (width / 8).toFloat()
            var cx = radius
            val lineOffset = dp2px(MAGNIFIER_BORDER_WIDTH).toInt()
            mMagnifierDrawable!!.setBounds(
                lineOffset,
                lineOffset,
                radius.toInt() * 2 - lineOffset,
                radius.toInt() * 2 - lineOffset
            )
            val pointsDistance: Double = CropUtils.getPointsDistance(draggingX, draggingY, 0f, 0f)
            if (pointsDistance < radius * 2.5) {
                mMagnifierDrawable!!.setBounds(
                    width - radius.toInt() * 2 + lineOffset,
                    lineOffset,
                    width - lineOffset,
                    radius.toInt() * 2 - lineOffset
                )
                cx = width - radius
            }
            canvas.drawCircle(cx, radius, radius, mMagnifierPaint!!)
            mMagnifierMatrix.setTranslate(radius - draggingX, radius - draggingY)
            mMagnifierDrawable!!.paint.shader.setLocalMatrix(mMagnifierMatrix)
            mMagnifierDrawable!!.draw(canvas)
            val crossLength = dp2px(MAGNIFIER_CROSS_LINE_LENGTH)
            canvas.drawLine(
                cx, radius - crossLength, cx, radius + crossLength,
                mMagnifierCrossPaint!!
            )
            canvas.drawLine(
                cx - crossLength, radius, cx + crossLength, radius,
                mMagnifierCrossPaint!!
            )
        }
    }

    private fun onDrawMask(canvas: Canvas) {
        if (mMaskAlpha <= 0) {
            return
        }
        val path = resetPointPath()
        if (path != null) {
            val sc = canvas.saveLayer(
                mActLeft.toFloat(),
                mActTop.toFloat(),
                (mActLeft + mActWidth).toFloat(),
                (mActTop + mActHeight).toFloat(),
                mMaskPaint,
                Canvas.ALL_SAVE_FLAG
            )
            mMaskPaint!!.alpha = mMaskAlpha
            canvas.drawRect(
                mActLeft.toFloat(),
                mActTop.toFloat(),
                (mActLeft + mActWidth).toFloat(),
                (mActTop + mActHeight).toFloat(),
                mMaskPaint!!
            )
            mMaskPaint!!.xfermode = mMaskXfermode
            mMaskPaint!!.alpha = 255
            canvas.drawPath(path, mMaskPaint!!)
            mMaskPaint!!.xfermode = null
            canvas.restoreToCount(sc)
        }
    }

    private fun resetPointPath(): Path? {
        if (!checkPoints(mCropPoints)) {
            return null
        }
        mPointLinePath.reset()
        val lt = mCropPoints[0]
        val rt = mCropPoints[1]
        val rb = mCropPoints[2]
        val lb = mCropPoints[3]
        mPointLinePath.moveTo(getViewPointX(lt), getViewPointY(lt))
        mPointLinePath.lineTo(getViewPointX(rt), getViewPointY(rt))
        mPointLinePath.lineTo(getViewPointX(rb), getViewPointY(rb))
        mPointLinePath.lineTo(getViewPointX(lb), getViewPointY(lb))
        mPointLinePath.close()
        return mPointLinePath
    }

    private val drawablePosition: Unit
        get() {
            val drawable = drawable
            if (drawable != null) {
                imageMatrix.getValues(mMatrixValue)
                mScaleX = mMatrixValue[Matrix.MSCALE_X]
                mScaleY = mMatrixValue[Matrix.MSCALE_Y]
                val origW = drawable.intrinsicWidth
                val origH = drawable.intrinsicHeight
                mActWidth = (origW * mScaleX).roundToInt()
                mActHeight = (origH * mScaleY).roundToInt()
                mActLeft = (width - mActWidth) / 2
                mActTop = (height - mActHeight) / 2
            }
        }

    private fun onDrawLines(canvas: Canvas) {
        val path = resetPointPath()
        if (path != null) {
            canvas.drawPath(path, mLinePaint!!)
        }
    }

    private fun onDrawPoints(canvas: Canvas) {
        if (!checkPoints(mCropPoints)) {
            return
        }
        for (point in mCropPoints) {
            canvas.drawCircle(
                getViewPointX(point), getViewPointY(point), dp2px(POINT_RADIUS),
                mPointFillPaint!!
            )
            canvas.drawCircle(
                getViewPointX(point), getViewPointY(point), dp2px(POINT_RADIUS),
                mPointPaint!!
            )
        }
        if (mShowEdgeMidPoint) {
            setEdgeMidPoints()
            //中间锚点
            for (point in mEdgeMidPoints) {
                canvas.drawCircle(
                    getViewPointX(point), getViewPointY(point), dp2px(POINT_RADIUS),
                    mPointFillPaint!!
                )
                canvas.drawCircle(
                    getViewPointX(point), getViewPointY(point), dp2px(POINT_RADIUS),
                    mPointPaint!!
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        var handle = true
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mDraggingPoint = getNearbyPoint(event)
                if (mDraggingPoint == null) {
                    handle = false
                }
            }
            MotionEvent.ACTION_MOVE -> toImagePointSize(mDraggingPoint, event)
            MotionEvent.ACTION_UP -> mDraggingPoint = null
        }
        invalidate()
        return handle || super.onTouchEvent(event)
    }

    private fun getNearbyPoint(event: MotionEvent): Point? {
        if (checkPoints(mCropPoints)) {
            for (p in mCropPoints) {
                if (isTouchPoint(p, event)) return p
            }
        }
        if (checkPoints(mEdgeMidPoints)) {
            for (p in mEdgeMidPoints) {
                if (isTouchPoint(p, event)) return p
            }
        }
        return null
    }

    private fun isTouchPoint(p: Point?, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val px = getViewPointX(p)
        val py = getViewPointY(p)
        val distance =
            sqrt((x - px).toDouble().pow(2.0) + (y - py).toDouble().pow(2.0))
        return distance < dp2px(TOUCH_POINT_CATCH_DISTANCE)
    }

    private fun toImagePointSize(dragPoint: Point?, event: MotionEvent) {
        if (dragPoint == null) {
            return
        }
        val pointType = getPointType(dragPoint)
        val x = ((min(
            max(event.x, mActLeft.toFloat()),
            (mActLeft + mActWidth).toFloat()
        ) - mActLeft) / mScaleX).toInt()
        val y = ((min(
            max(event.y, mActTop.toFloat()),
            (mActTop + mActHeight).toFloat()
        ) - mActTop) / mScaleY).toInt()
        if (mDragLimit && pointType != null) {
            when (pointType) {
                DragPointType.LEFT_TOP -> if (!canMoveLeftTop(x, y)) return
                DragPointType.RIGHT_TOP -> if (!canMoveRightTop(x, y)) return
                DragPointType.RIGHT_BOTTOM -> if (!canMoveRightBottom(x, y)) return
                DragPointType.LEFT_BOTTOM -> if (!canMoveLeftBottom(x, y)) return
                DragPointType.TOP -> if (!canMoveLeftTop(x, y) || !canMoveRightTop(x, y)) return
                DragPointType.RIGHT -> if (!canMoveRightTop(x, y) || !canMoveRightBottom(
                        x,
                        y
                    )
                ) return
                DragPointType.BOTTOM -> if (!canMoveLeftBottom(x, y) || !canMoveRightBottom(
                        x,
                        y
                    )
                ) return
                DragPointType.LEFT -> if (!canMoveLeftBottom(x, y) || !canMoveLeftTop(x, y)) return
            }
        }
        if (DragPointType.isEdgePoint(pointType)) {
            val xoff = x - dragPoint.x
            val yoff = y - dragPoint.y
            moveEdge(pointType, xoff, yoff)
        } else {
            dragPoint.y = y
            dragPoint.x = x
        }
    }

    private fun moveEdge(type: DragPointType?, xoff: Int, yoff: Int) {
        when (type) {
            DragPointType.TOP -> {
                movePoint(mCropPoints[P_LT], 0, yoff)
                movePoint(mCropPoints[P_RT], 0, yoff)
            }
            DragPointType.RIGHT -> {
                movePoint(mCropPoints[P_RT], xoff, 0)
                movePoint(mCropPoints[P_RB], xoff, 0)
            }
            DragPointType.BOTTOM -> {
                movePoint(mCropPoints[P_LB], 0, yoff)
                movePoint(mCropPoints[P_RB], 0, yoff)
            }
            DragPointType.LEFT -> {
                movePoint(mCropPoints[P_LT], xoff, 0)
                movePoint(mCropPoints[P_LB], xoff, 0)
            }
            else -> {}
        }
    }

    private fun movePoint(point: Point?, xoff: Int, yoff: Int) {
        if (point == null) return
        val x = point.x + xoff
        val y = point.y + yoff
        if (x < 0 || x > drawable.intrinsicWidth) return
        if (y < 0 || y > drawable.intrinsicHeight) return
        point.x = x
        point.y = y
    }

    private fun canMoveLeftTop(x: Int, y: Int): Boolean {
        if (pointSideLine(
                mCropPoints[P_RT],
                mCropPoints[P_LB], x, y
            )
            * pointSideLine(
                mCropPoints[P_RT],
                mCropPoints[P_LB], mCropPoints[P_RB]
            ) > 0
        ) {
            return false
        }
        if (pointSideLine(
                mCropPoints[P_RT],
                mCropPoints[P_RB], x, y
            )
            * pointSideLine(
                mCropPoints[P_RT],
                mCropPoints[P_RB], mCropPoints[P_LB]
            ) < 0
        ) {
            return false
        }
        return (pointSideLine(
            mCropPoints[P_LB],
            mCropPoints[P_RB], x, y
        )
                * pointSideLine(
            mCropPoints[P_LB],
            mCropPoints[P_RB], mCropPoints[P_RT]
        )) >= 0
    }

    private fun canMoveRightTop(x: Int, y: Int): Boolean {
        if (pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_RB], x, y
            )
            * pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_RB], mCropPoints[P_LB]
            ) > 0
        ) {
            return false
        }
        if (pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_LB], x, y
            )
            * pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_LB], mCropPoints[P_RB]
            ) < 0
        ) {
            return false
        }
        return (pointSideLine(
            mCropPoints[P_LB],
            mCropPoints[P_RB], x, y
        )
                * pointSideLine(
            mCropPoints[P_LB],
            mCropPoints[P_RB], mCropPoints[P_LT]
        )) >= 0
    }

    private fun canMoveRightBottom(x: Int, y: Int): Boolean {
        if (pointSideLine(
                mCropPoints[P_RT],
                mCropPoints[P_LB], x, y
            )
            * pointSideLine(
                mCropPoints[P_RT],
                mCropPoints[P_LB], mCropPoints[P_LT]
            ) > 0
        ) {
            return false
        }
        if (pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_RT], x, y
            )
            * pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_RT], mCropPoints[P_LB]
            ) < 0
        ) {
            return false
        }
        return (pointSideLine(
            mCropPoints[P_LT],
            mCropPoints[P_LB], x, y
        )
                * pointSideLine(
            mCropPoints[P_LT],
            mCropPoints[P_LB], mCropPoints[P_RT]
        )) >= 0
    }

    private fun canMoveLeftBottom(x: Int, y: Int): Boolean {
        if (pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_RB], x, y
            )
            * pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_RB], mCropPoints[P_RT]
            ) > 0
        ) {
            return false
        }
        if (pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_RT], x, y
            )
            * pointSideLine(
                mCropPoints[P_LT],
                mCropPoints[P_RT], mCropPoints[P_RB]
            ) < 0
        ) {
            return false
        }
        return (pointSideLine(
            mCropPoints[P_RT],
            mCropPoints[P_RB], x, y
        )
                * pointSideLine(
            mCropPoints[P_RT],
            mCropPoints[P_RB], mCropPoints[P_LT]
        )) >= 0
    }

    private fun getPointType(dragPoint: Point?): DragPointType? {
        if (dragPoint == null) return null
        val type: DragPointType
        if (checkPoints(mCropPoints)) {
            for (i in mCropPoints.indices) {
                if (dragPoint === mCropPoints[i]) {
                    type = DragPointType.values()[i]
                    return type
                }
            }
        }
        if (checkPoints(mEdgeMidPoints)) {
            for (i in mEdgeMidPoints.indices) {
                if (dragPoint === mEdgeMidPoints[i]) {
                    type = DragPointType.values()[4 + i]
                    return type
                }
            }
        }
        return null
    }

    private fun getViewPointX(point: Point?): Float {
        return point!!.x * mScaleX + mActLeft
    }

    private fun getViewPointY(point: Point?): Float {
        return point!!.y * mScaleY + mActTop
    }

    private fun dp2px(dp: Float): Float {
        return dp * mDensity
    }

    private val fullImgCropPoints: Array<Point?>
        get() {
            val points = arrayOfNulls<Point>(4)
            val drawable = drawable
            if (drawable != null) {
                val width = drawable.intrinsicWidth
                val height = drawable.intrinsicHeight
                points[0] = Point(0, 0)
                points[1] = Point(width, 0)
                points[2] = Point(width, height)
                points[3] = Point(0, height)
            }
            return points
        }

    companion object {
        private const val TAG = "CropImageView"
        private const val TOUCH_POINT_CATCH_DISTANCE = 15f //dp，触摸点捕捉到锚点的最小距离
        private const val POINT_RADIUS = 10f // dp，锚点绘制半价
        private const val MAGNIFIER_CROSS_LINE_WIDTH = 0.8f //dp，放大镜十字宽度
        private const val MAGNIFIER_CROSS_LINE_LENGTH = 3f //dp， 放大镜十字长度
        private const val MAGNIFIER_BORDER_WIDTH = 1f //dp，放大镜边框宽度
        private const val DEFAULT_LINE_COLOR = -0xff0001
        private const val DEFAULT_LINE_WIDTH = 1f //dp
        private const val DEFAULT_MASK_ALPHA = 86 // 0 - 255
        private const val DEFAULT_MAGNIFIER_CROSS_COLOR = -0xbf7f
        private const val DEFAULT_POINT_FILL_ALPHA = 175
        private const val P_LT = 0
        private const val P_RT = 1
        private const val P_RB = 2
        private const val P_LB = 3
    }

    init {
        val scaleType = scaleType
        if (scaleType == ScaleType.FIT_END || scaleType == ScaleType.FIT_START || scaleType == ScaleType.MATRIX) {
            throw RuntimeException("Image in CropImageView must be in center")
        }
        mDensity = resources.displayMetrics.density
        initAttrs(context, attrs)
        initPaints()
    }
}
