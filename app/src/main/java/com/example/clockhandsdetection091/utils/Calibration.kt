package com.example.clockhandsdetection091.utils

import com.example.clockhandsdetection091.Clocks
import com.example.clockhandsdetection091.enumeration.Event
import com.example.clockhandsdetection091.enumeration.State
import org.json.JSONArray
import org.json.JSONObject

/**
 * Class used for the calibration of each clocks of the matrice device.
 * @property [clockEvents] contain the event that will be processed every calibration() calls.
 * @property [previousState] contain the previous state of each clock.
 * @property [oldClocks] contain the old clock array. Of the previous calibration call.
 * @author Ruben De Campos
 */
class Calibration(matriceSize: Int, var jsonString: String) {

    private var oldClocks: Clocks = Clocks(matriceSize)
    private var previousState = MutableList(matriceSize, init = {State.INIT})
    var clockEvents = MutableList(matriceSize, init = {Event.EvDefault})

    /**
     * calibrate all the clocks to their position 0. It process each clocks "state machine".
     * @param [clockArray] the clockArray object that contain the list of all the clocks
     * @return contain the actions for the device to do (ex: rotating hand1 by 45°)
     */
    fun calibrate(clockArray: Clocks): JSONObject{
        var arrayJSON = JSONArray(jsonString)

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
                            if(previousState[i] == State.HAND_ID_UNKNOWN)
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
                            (arrayJSON[i] as JSONObject).put("moveWP1",45)
                            (arrayJSON[i] as JSONObject).put("moveWP2",0)
                        }else{
                            (arrayJSON[i] as JSONObject).put("moveWP1",180)
                            (arrayJSON[i] as JSONObject).put("moveWP2",0)
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
                            (arrayJSON[i] as JSONObject).put("moveWP1",360-clock.angle1)
                            (arrayJSON[i] as JSONObject).put("moveWP2",360-clock.angle2)
                        }
                    }
                    //-----------------------------------------------------------------------------
                    State.TOO_CLOSE -> {
                        // The hands are to close to be detected correctly.
                        // Move one hand (except if this clock was already calibrated).
                        (arrayJSON[i] as JSONObject).put("moveWP1",180)
                        (arrayJSON[i] as JSONObject).put("moveWP2",0)

                        // Generate the default event
                        clockEvents[i] = Event.EvDefault
                    }
                    //-----------------------------------------------------------------------------
                    State.NOT_DETECTED -> {
                        // State NOT_DETECTED do nothing, it wait for the next iteration
                        previousState[i] = oldClock.state

                        // Generate the default event
                        clockEvents[i] = Event.EvDefault
                    }
                }
            }
        }

        clockArray.copyTo(oldClocks)

        // Create the json object to be send
        val jsonObject = JSONObject()
        jsonObject.put("header","CALIBRATION")
        jsonObject.put("body", arrayJSON)

        return jsonObject
    }
}