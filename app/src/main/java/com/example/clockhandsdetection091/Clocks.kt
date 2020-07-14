package com.example.clockhandsdetection091

import com.example.clockhandsdetection091.enumeration.State
import org.opencv.core.Point

class Clocks(size: Int) {

    var clocks: ArrayList<Clock> = ArrayList()

    init {
        for (i in 0 until size){
            clocks.add(Clock())
        }
    }

    // Copy the content of clocks into a given Clocks object
    fun copyTo(clockArray: Clocks){
        if(clockArray.clocks.size == clocks.size) {
            for (i in 0 until clocks.size) {
                clockArray.clocks[i].state = clocks[i].state
                clockArray.clocks[i].calibrated = clocks[i].calibrated
                clockArray.clocks[i].center = clocks[i].center
                clockArray.clocks[i].angle1 = clocks[i].angle1
                clockArray.clocks[i].angle2 = clocks[i].angle2
            }
        }
    }

    override fun toString(): String{
        var string = ""

        for(i in clocks.indices){
            string += "Clock "+ i +" ["+clocks[i].state+"] : " +
                    clocks[i].angle1 + " / " + clocks[i].angle2 + "\n"
        }

        return string
    }

    class Clock{
        var state: State = State.INIT
        var calibrated = false
        var center: Point = Point()
        var angle1 = 0
        var angle2 = 0
    }
}

