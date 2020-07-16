package com.example.clockhandsdetection091.utils

import android.graphics.Bitmap
import com.example.clockhandsdetection091.Clocks
import org.opencv.core.Mat

open class HandsDetection {
    var bmpMatrice: Bitmap? = null
    var matriceMat: Mat? = null
    val matList: MutableList<Mat?> = mutableListOf()
    var clockArray: Clocks? = null
    var calibrate: Calibration? = null
    var currentPicturePath: String = ""
    var isComputed = false
}