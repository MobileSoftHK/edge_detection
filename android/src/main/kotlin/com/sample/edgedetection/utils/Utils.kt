package com.sample.edgedetection.utils

import org.opencv.core.Point
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * Created by Matej Danicek on 13.12.21.
 */

const val DEFAULT_POINT_MARGIN_RATIO = 0.1
const val CLOSE_POINTS_WIDTH_RATIO = 0.15

/**
 * Distance from this to another point
 */
fun Point.distanceTo(that: Point): Double {
    val deltaX = this.x - that.x
    val deltaY = this.y - that.y
    return sqrt(deltaX.pow(2.0) + deltaY.pow(2.0))
}