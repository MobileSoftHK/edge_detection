package com.sample.edgedetection.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.sample.edgedetection.processor.Corners
import com.sample.edgedetection.processor.TAG
import com.sample.edgedetection.utils.CLOSE_POINTS_WIDTH_RATIO
import com.sample.edgedetection.utils.DEFAULT_POINT_MARGIN_RATIO
import com.sample.edgedetection.utils.distanceTo
import org.opencv.core.Point
import org.opencv.core.Size


class PaperRectangle : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributes: AttributeSet) : super(context, attributes)
    constructor(context: Context, attributes: AttributeSet, defTheme: Int) : super(
        context,
        attributes,
        defTheme
    )

    private val rectPaint = Paint()
    private val circlePaint = Paint()
    private var ratioX: Double = 1.0
    private var ratioY: Double = 1.0
    private var tl: Point = Point()
    private var tr: Point = Point()
    private var br: Point = Point()
    private var bl: Point = Point()
    private val path: Path = Path()
    private var point2Move = Point()
    private var cropMode = false
    private var latestDownX = 0.0F
    private var latestDownY = 0.0F

    init {
        rectPaint.color = Color.WHITE
        rectPaint.isAntiAlias = true
        rectPaint.isDither = true
        rectPaint.strokeWidth = 6F
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeJoin = Paint.Join.ROUND    // set the join to round you want
        rectPaint.strokeCap = Paint.Cap.ROUND      // set the paint cap to round too
        rectPaint.pathEffect = CornerPathEffect(10f)

        circlePaint.color = Color.LTGRAY
        circlePaint.isDither = true
        circlePaint.isAntiAlias = true
        circlePaint.strokeWidth = 4F
        circlePaint.style = Paint.Style.STROKE
    }

    fun onCornersDetected(corners: Corners) {
        ratioX = corners.size.width.div(measuredWidth)
        ratioY = corners.size.height.div(measuredHeight)
        tl = corners.corners[0] ?: Point()
        tr = corners.corners[1] ?: Point()
        br = corners.corners[2] ?: Point()
        bl = corners.corners[3] ?: Point()

        Log.i(TAG, "POINTS ------>  $tl corners")

        resize()
        path.reset()
        path.moveTo(tl.x.toFloat(), tl.y.toFloat())
        path.lineTo(tr.x.toFloat(), tr.y.toFloat())
        path.lineTo(br.x.toFloat(), br.y.toFloat())
        path.lineTo(bl.x.toFloat(), bl.y.toFloat())
        path.close()
        invalidate()
    }

    fun onCornersNotDetected() {
        path.reset()
        invalidate()
    }

    fun onCorners2Crop(corners: Corners?, size: Size?, paperWidth: Int, paperHeight: Int) {
        cropMode = true

        val pictureWidth = size?.width?.toInt() ?: paperWidth // just in case elvis
        val pictureHeight = size?.height?.toInt() ?: paperHeight // just in case elvis

        val points =
            if (shouldDefaultPoints(corners, pictureWidth, pictureHeight)) {
                getDefaultPoints(pictureWidth, pictureHeight)
            } else {
                corners?.corners ?: getDefaultPoints(pictureWidth, pictureHeight)
            }

        tl = points[0]!!
        tr = points[1]!!
        br = points[2]!!
        bl = points[3]!!

        ratioX = size?.width?.div(paperWidth) ?: 1.0
        ratioY = size?.height?.div(paperHeight) ?: 1.0
        resize()
        movePoints()
    }

    /**
     * @param width the width of the picture to use as a base for calculating the minimal-point-distance limit
     * @param height (for future use) the height of the picture to use as a base for calculating the minimal-point-distance limit
     * @return true if the provided [corners] are null, their size is not 4 or the distance between the two closest point is less that a given relative limit (see [CLOSE_POINTS_WIDTH_RATIO])
     */
    private fun shouldDefaultPoints(corners: Corners?, width: Int, height: Int): Boolean {
        if (corners == null) return true

        val points = corners.corners.filterNotNull()

        if (points.size != 4) return true

        val distances: HashSet<Int> = HashSet()
        points.forEachIndexed { i, point ->
            for (j in i+1 until corners.corners.size) {
                distances.add(point.distanceTo(points[j]).toInt())
            }
        }

        return distances.min()!! < (width * CLOSE_POINTS_WIDTH_RATIO)
    }

    /**
     * @param width the width of the picture to use as an anchor for the default points
     * @param height the height of the picture to use as an anchor for the default points
     * @return ordered (tl, tr, br, bl) list of default point positioned in the corners of the picture with some static margins (see [DEFAULT_POINT_MARGIN_RATIO])
     */
    private fun getDefaultPoints(width: Int, height: Int): List<Point> {
        val marginX = width * DEFAULT_POINT_MARGIN_RATIO
        val marginY = marginX

        val points = mutableListOf<Point>()
        points.add(Point(marginX, marginY))
        points.add(Point(width - marginX, marginY))
        points.add(Point(width - marginX, height - marginY))
        points.add(Point(marginX, height - marginY))

        return points
    }

    fun getCorners2Crop(): List<Point> {
        reverseSize()
        return listOf(tl, tr, br, bl)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawPath(path, rectPaint)
        if (cropMode) {
            canvas?.drawCircle(tl.x.toFloat(), tl.y.toFloat(), 20F, circlePaint)
            canvas?.drawCircle(tr.x.toFloat(), tr.y.toFloat(), 20F, circlePaint)
            canvas?.drawCircle(bl.x.toFloat(), bl.y.toFloat(), 20F, circlePaint)
            canvas?.drawCircle(br.x.toFloat(), br.y.toFloat(), 20F, circlePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (!cropMode) {
            return false
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                latestDownX = event.x
                latestDownY = event.y
                calculatePoint2Move(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                point2Move.x = (event.x - latestDownX) + point2Move.x
                point2Move.y = (event.y - latestDownY) + point2Move.y
                movePoints()
                latestDownY = event.y
                latestDownX = event.x
            }
        }
        return true
    }

    private fun calculatePoint2Move(downX: Float, downY: Float) {
        val points = listOf(tl, tr, br, bl)
        point2Move = points.minBy { Math.abs((it.x - downX).times(it.y - downY)) } ?: tl
    }

    private fun movePoints() {
        path.reset()
        path.moveTo(tl.x.toFloat(), tl.y.toFloat())
        path.lineTo(tr.x.toFloat(), tr.y.toFloat())
        path.lineTo(br.x.toFloat(), br.y.toFloat())
        path.lineTo(bl.x.toFloat(), bl.y.toFloat())
        path.close()
        invalidate()
    }


    private fun resize() {
        tl.x = tl.x.div(ratioX)
        tl.y = tl.y.div(ratioY)
        tr.x = tr.x.div(ratioX)
        tr.y = tr.y.div(ratioY)
        br.x = br.x.div(ratioX)
        br.y = br.y.div(ratioY)
        bl.x = bl.x.div(ratioX)
        bl.y = bl.y.div(ratioY)
    }

    private fun reverseSize() {
        tl.x = tl.x.times(ratioX)
        tl.y = tl.y.times(ratioY)
        tr.x = tr.x.times(ratioX)
        tr.y = tr.y.times(ratioY)
        br.x = br.x.times(ratioX)
        br.y = br.y.times(ratioY)
        bl.x = bl.x.times(ratioX)
        bl.y = bl.y.times(ratioY)
    }
}