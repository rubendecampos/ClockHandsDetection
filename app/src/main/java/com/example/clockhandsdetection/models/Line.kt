package com.example.clockhandsdetection.models

import org.opencv.core.Point

/**
 * Class Line, constituted of two points.
 * @property [p1] first point of the line.
 * @property [p2] second point of the line.
 * @author Ruben De Campos
 */
class Line(p1: Point, p2: Point) {

    var p1: Point
    var p2: Point

    init {
        this.p1 = p1
        this.p2 = p2
    }

    constructor() : this(Point(0.0,0.0),Point(0.0,0.0)) {
        this.p1 = p1
        this.p2 = p2
    }

    /**
     * Set p1 and p2 with a line in a DoubleArray form.
     * @param [lineInDoubleArray] line in a DoubleArray form.
     */
    fun setLine(lineInDoubleArray: DoubleArray){
        p1.x = lineInDoubleArray[0]
        p1.y = lineInDoubleArray[1]
        p2.x = lineInDoubleArray[2]
        p2.y = lineInDoubleArray[3]
    }
}