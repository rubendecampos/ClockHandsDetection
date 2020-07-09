package com.example.clockhandsdetection091

import com.example.clockhandsdetection091.Enum.State

class Clocks(size: Int) {

    var clocks: ArrayList<Clock> = ArrayList()

    init {
        for (i in 0 until size){
            clocks.add(Clock())
        }
    }

    class Clock{
        var state: State = State.NONE
        var angle1 = 0
        var angle2 = 0
    }
}

