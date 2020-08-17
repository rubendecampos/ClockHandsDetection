package com.example.clockhandsdetection.models

import com.example.clockhandsdetection.enumeration.State
import org.opencv.core.Point

/**
 * Class that contain the list of clocks.
 * Each clock has a state, a center, two angles for the hands, and if it has been calibrated.
 * @property [clocks] list of clocks.
 * @author Ruben De Campos
 */
class Clocks(size: Int) {

    var clocks: ArrayList<Clock> = ArrayList()

    init {
        for (i in 0 until size){
            clocks.add(Clock())
        }
    }

    /**
     * Copy the content of clocks into a given Clocks object.
     * @param [clockArray] array to copy the clock's list into.
     */
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

    /**
     * @return a string of the clock's list.
     */
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

