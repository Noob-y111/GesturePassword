package com.example.gesturepassword

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.util.Range
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt

class GesturePassword : View {

    companion object {
        const val TAG = "GesturePassword"
    }

    data class Point(var x: Int, var y: Int)
    data class CircularRange(var point: Point, var radius: Float) {
        fun returnXRange(): Range<Float> {
            return Range(point.x - radius, point.x + radius)
        }

        fun returnYRange(): Range<Float> {
            return Range(point.y - radius, point.y + radius)
        }
    }

    // 手势解锁结束回调
    private var onLockOverCallback: ((Array<Point>) -> Unit)? = null

    // 圆形坐标范围
    private val circularRanges = ArrayList<CircularRange>()

    // 是否已经被触摸过
    private val circleIsTouched = Array(9) {
        return@Array false
    }

    // 解锁顺序
    private val orderOfMove = ArrayList<Point>()

    // 填充颜色 线条颜色
    @ColorInt
    private var fillColor = Color.GREEN

    @ColorInt
    private var lineColor = Color.GREEN

    // 最小宽度
    private var minWidth = dpToPx(100f)

    // 圆形半径
    private var radius: Float? = null

    // 触摸事件是否结束
    private var isTouched = false

    // 手指位置坐标
    private var moveXY = Point(0, 0)

    private var canvas: Canvas? = null

    private val circlePaint = Paint().also {
        it.isAntiAlias = true
        it.style = Paint.Style.STROKE
        it.strokeWidth = dpToPx(3f).toFloat()
    }

    private val fillCirclePaint = Paint().also {
        it.isAntiAlias = true
        it.style = Paint.Style.FILL
    }

    private val linePaint = Paint().also {
        it.isAntiAlias = true
        it.style = Paint.Style.STROKE
        it.strokeWidth = dpToPx(4f).toFloat()
    }

    private fun dpToPx(float: Float): Int {
        return ((resources.displayMetrics.density) * float + 0.5f).toInt()
    }

    private fun pxToDp(float: Float): Int {
        return (float / (resources.displayMetrics.density) + 0.5f).toInt()
    }

    fun setOnLockOverListener(onLockOverCallback: ((Array<Point>) -> Unit)?) {
        this.onLockOverCallback = onLockOverCallback
    }

    override fun performClick() = super.performClick()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        getAttrs(context, attrs)
    }

    private fun getAttrs(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.GesturePassword).apply {
            fillColor = getColor(R.styleable.GesturePassword_gestureFillColor, fillColor)
            lineColor = getColor(R.styleable.GesturePassword_gestureLineColor, lineColor)
            recycle()
        }
        initPaint()
    }

    private fun initPaint() {
        circlePaint.color = lineColor
        fillCirclePaint.color = fillColor
        linePaint.color = lineColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        Log.d(TAG, "onMeasure: width ==> $width")

        if (width < minWidth) {
            val circleWidthSpec = MeasureSpec.makeMeasureSpec(minWidth, MeasureSpec.EXACTLY)
            setMeasuredDimension(circleWidthSpec, circleWidthSpec)
        } else {
            minWidth = width
            setMeasuredDimension(widthMeasureSpec, widthMeasureSpec)
        }
    }


    private fun getRadius(): Float = radius ?: kotlin.run {
        radius = ((minWidth.toFloat() / 3) - dpToPx(minWidth.toFloat() / 30f)) / 2
        radius!!
    }

    // 计算每个圆形的位置
    private fun getPoint(it: Int): Point {
        val width = minWidth / 3
        return Point((it % 3) * width + (width / 2), (it / 3) * width + (width / 2))
    }

    private fun drawCircle(point: Point) {
        canvas?.drawCircle(point.x.toFloat(), point.y.toFloat(), getRadius(), circlePaint)
        canvas?.drawCircle(
            point.x.toFloat(),
            point.y.toFloat(),
            dpToPx(minWidth / 120f).toFloat(),
            fillCirclePaint
        )
        addElement(point)
    }

    private fun addElement(point: Point) {
        if (circularRanges.size < 9) circularRanges.add(CircularRange(point, getRadius()))
    }

    override fun onDraw(canvas: Canvas?) {
        this.canvas = canvas
        repeat(9) {
            drawCircle(getPoint(it))
        }
        circleIsTouched.forEachIndexed { index, b ->
            if (b) {
                drawFillCircle(circularRanges[index].point)
            }
        }
        if ((orderOfMove.size != 0) && isTouched) {
            drawLinePointToPoint()
            drawLineForPointToMoveXY(orderOfMove.last(), fingerPoint = moveXY)
        }
    }

    // 两个圆形之间的连线
    private fun drawLinePointToPoint() {
        if (orderOfMove.size <= 1) return
        for (i in 1 until orderOfMove.size) {
            drawLineForPointToMoveXY(orderOfMove[i - 1], orderOfMove[i])
        }
    }

    // 最后一个圆形和手指位置的连线
    private fun drawLineForPointToMoveXY(lastPoint: Point, fingerPoint: Point) {
        canvas?.drawLine(
            lastPoint.x.toFloat(),
            lastPoint.y.toFloat(),
            fingerPoint.x.toFloat(),
            fingerPoint.y.toFloat(),
            linePaint
        )
    }

    private fun drawFillCircle(point: Point) {
        canvas?.drawCircle(point.x.toFloat(), point.y.toFloat(), getRadius(), fillCirclePaint)
    }

    // 手指位置是否在9个圆形范围内
    private fun inRanges(x: Float, y: Float) {
        for ((index, it) in circularRanges.withIndex()) {
            if (it.returnXRange().contains(x) && it.returnYRange().contains(y)) {
                if (!circleIsTouched[index]) {
                    resetBooleanArray(index)
                    orderOfMove.add(circularRanges[index].point)
                }
                break
            }
        }
    }

    // 被触摸的圆形下标置为true
    private fun resetBooleanArray(index: Int) {
        if (index >= 0) {
            circleIsTouched[index] = true
        } else {
            orderOfMove.clear()
            for (i in circleIsTouched.indices) {
                circleIsTouched[i] = false
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    isTouched = true
                    moveXY.x = motionEvent.x.toInt()
                    moveXY.y = motionEvent.y.toInt()
                    inRanges(motionEvent.x, motionEvent.y)
                }
                MotionEvent.ACTION_MOVE -> {
                    moveXY.x = motionEvent.x.toInt()
                    moveXY.y = motionEvent.y.toInt()
                    inRanges(motionEvent.x, motionEvent.y)
                }
                MotionEvent.ACTION_UP -> {
                    isTouched = false
                    moveXY.x = motionEvent.x.toInt()
                    moveXY.y = motionEvent.y.toInt()
                    onLockOverCallback?.let {
                        it(Array(orderOfMove.size) {
                            return@Array orderOfMove[it]
                        })
                    }
                    resetBooleanArray(-1)
                }
            }
        }
        invalidate()
        performClick()
        return true
    }
}