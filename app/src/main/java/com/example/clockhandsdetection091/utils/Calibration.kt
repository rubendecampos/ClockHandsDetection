package com.example.clockhandsdetection091.utils

import com.example.clockhandsdetection091.Clocks
import com.example.clockhandsdetection091.enumeration.Event
import com.example.clockhandsdetection091.enumeration.State

class Calibration(matriceSize: Int) {

    private var oldClocks: Clocks = Clocks(matriceSize)
    private var previousState = State.INIT
    var clockEvents = MutableList(matriceSize, init = {Event.EvDefault})

    fun calibration(clockArray: Clocks): String{
        var actions = ""

        for(i in 0 until clockArray.clocks.size){
            val clock = clockArray.clocks[i]
            val oldClock = oldClocks.clocks[i]
            val event = clockEvents[i]

            // Transition switch
            when(clock.state){
                State.INIT -> {
                    when(event){
                        Event.EvDefault -> clock.state = State.HAND_ID_UNKNOWN
                        Event.EvTooCLose -> clock.state = State.TOO_CLOSE
                        Event.EvNotDetected -> clock.state = State.NOT_DETECTED
                    }
                }
                //---------------------------------------------------------------------------------
                State.HAND_ID_UNKNOWN -> {
                    when(event){
                        Event.EvDefault -> clock.state = State.HAND_ID_KNOWN
                        Event.EvTooCLose -> clock.state = State.TOO_CLOSE
                        Event.EvNotDetected -> clock.state = State.NOT_DETECTED
                    }
                }
                //---------------------------------------------------------------------------------
                State.HAND_ID_KNOWN -> {
                    // If calibrated, end the state machine. Else continue it
                    if(clock.calibrated){
                        clock.state = State.CALIBRATED
                    }else{
                        when(event){
                            Event.EvDefault -> clock.state = State.HAND_ID_UNKNOWN
                            Event.EvTooCLose -> clock.state = State.TOO_CLOSE
                            Event.EvNotDetected -> clock.state = State.NOT_DETECTED
                        }
                    }
                }
                //---------------------------------------------------------------------------------
                State.TOO_CLOSE -> {
                    when(event){
                        Event.EvDefault -> clock.state = State.HAND_ID_UNKNOWN
                        Event.EvTooCLose -> clock.state = State.TOO_CLOSE
                        Event.EvNotDetected -> clock.state = State.NOT_DETECTED
                    }
                }
                //---------------------------------------------------------------------------------
                State.NOT_DETECTED -> {
                    // To continue the calibration, the next state depend of the previous state.
                    // Except for the tooClose and notDetected events.
                    when(event){
                        Event.EvTooCLose -> clock.state = State.TOO_CLOSE
                        Event.EvNotDetected -> clock.state = State.NOT_DETECTED
                        Event.EvDefault -> {
                            if(previousState == State.HAND_ID_UNKNOWN)
                                clock.state = State.HAND_ID_KNOWN
                            else
                                clock.state = State.HAND_ID_UNKNOWN
                        }
                    }
                }
            }
            // Action switch
            if(oldClock.state != clock.state){
                when(clock.state){
                    State.INIT -> {
                        // Generate the default event
                        clockEvents[i] = Event.EvDefault
                    }
                    //----------------------------------------------------------------------------
                    State.HAND_ID_UNKNOWN -> {
                        // First step in the calibration.
                        // We don't know which angle belong to which hand, so wo move the hand 1 to
                        // detect it in the next step.
                        if(Tools.handsAngle(
                                clock.angle1,
                                clock.angle2
                            ) > 150){
                            actions += "Clock "+i+": "+"hand 1 -> 45° / hand 2 -> 0°\n"
                        }else{
                            actions += "Clock "+i+": "+"hand 1 -> 180° / hand 2 -> 0°\n"
                        }

                        // Generate the default event
                        clockEvents[i] = Event.EvDefault
                    }
                    //----------------------------------------------------------------------------
                    State.HAND_ID_KNOWN -> {
                        // Second step of the calibration.
                        // The hand that has been moved is hand 1 and the other is hand 2. So now
                        // that both hands angle are known, move them to 0.

                        // Angle 2 is different to oldAngle 2 and angle 1 is similar to oldAngle 1
                        if(Tools.handsAngle(clock.angle2, oldClock.angle2) > 10 &&
                            Tools.handsAngle(clock.angle1, oldClock.angle1) <= 10){
                            val temp = clock.angle2
                            clock.angle2 = clock.angle1
                            clock.angle1 = temp
                            clock.calibrated = true

                        // Angle 2 is different to oldAngle 1 and angle 1 is similar to oldAngle 2
                        }else if (Tools.handsAngle(clock.angle2, oldClock.angle1) > 10 &&
                            Tools.handsAngle(clock.angle1, oldClock.angle2) <= 10){
                            val temp = clock.angle2
                            clock.angle2 = clock.angle1
                            clock.angle1 = temp
                            clock.calibrated = true

                        // Angle 1 is different to oldAngle 1 and angle 2 is similar to oldAngle 2
                        }else if (Tools.handsAngle(clock.angle1, oldClock.angle1) > 10 &&
                            Tools.handsAngle(clock.angle2, oldClock.angle2) <= 10){
                            // Already in order
                            clock.calibrated = true

                        // Angle 1 is different to oldAngle 2 and angle 2 is similar to oldAngle 1
                        }else if(Tools.handsAngle(clock.angle1, oldClock.angle2) > 10 &&
                            Tools.handsAngle(clock.angle2, oldClock.angle1) <= 10){
                            // Already in order
                            clock.calibrated = true
                        }else{
                        // If none of this cases have been found, restart the calibration of this
                        // clock.
                            clock.calibrated = false
                            clockEvents[i] = Event.EvDefault
                        }

                        // If the hands are correctly detected, init them to 0.
                        if(clock.calibrated){
                            actions += "Clock "+i+": "+"hand 1 -> "+(360-clock.angle1).toString()+
                                    " / hand 2 -> "+(360-clock.angle2).toString() + "\n"
                        }
                    }
                    //-----------------------------------------------------------------------------
                    State.TOO_CLOSE -> {
                        // The hands are to close to be detected correctly.
                        // Move one hand (except if this clock was already calibrated).
                        actions += "Clock "+i+": "+"hand 1 -> 180° / hand 2 -> 0°\n"

                        // Generate the default event
                        clockEvents[i] = Event.EvDefault
                    }
                    //-----------------------------------------------------------------------------
                    State.NOT_DETECTED -> {
                        // State NOT_DETECTED do nothing, it wait for the next iteration
                        previousState = oldClock.state

                        // Generate the default event
                        clockEvents[i] = Event.EvDefault
                    }
                }
            }
        }

        clockArray.copyTo(oldClocks)

        return actions
    }
}