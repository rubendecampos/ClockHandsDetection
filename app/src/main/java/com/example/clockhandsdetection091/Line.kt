package com.example.clockhandsdetection091

import org.opencv.core.Point

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

    fun setLine(lineInDoubleArray: DoubleArray){
        p1.x = lineInDoubleArray[0]
        p1.y = lineInDoubleArray[1]
        p2.x = lineInDoubleArray[2]
        p2.y = lineInDoubleArray[3]
    }
}