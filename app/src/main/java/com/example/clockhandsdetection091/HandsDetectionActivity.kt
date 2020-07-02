package com.example.clockhandsdetection091

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.util.*


class HandsDetectionActivity : AppCompatActivity() {

    val TAG = "HandsDetectionActivity"
    val MIN_CONTOURS_AREA = 1
    var MATRICE_ROW = 3
    var MATRICE_COL = 2
    var out: Mat? = null

    lateinit var ivPicture: ImageView
    lateinit var nbrContours: TextView
    lateinit var nbrCircles: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hands_detection)

        var btnCompute: Button = findViewById(R.id.btnCompute)
        nbrContours = findViewById(R.id.nbrContours)
        nbrCircles = findViewById(R.id.nbrCircles)
        ivPicture = findViewById(R.id.ivPicture)

        val intent = intent
        MATRICE_ROW = intent.getStringExtra("nbrRow").toInt()
        MATRICE_COL = intent.getStringExtra("nbrCol").toInt()
        val filePath = intent.getStringExtra("filePath")
        val bmp = BitmapFactory.decodeFile(filePath)

        // Find the contour of the matrice and apply a warpPerspective on it.
        var bmpMatrice = Tools.transformRectPerspective(bmp,
            MATRICE_COL/MATRICE_ROW.toDouble())
        ivPicture!!.setImageBitmap(bmpMatrice)

        // On the btnCompute click, start the detection
        btnCompute.setOnClickListener({
            detection(bmpMatrice)
        })
    }

    //------------------------------------------------------------------------------------
    // Complete code of the clock hands detection
    //------------------------------------------------------------------------------------
    fun detection(bmp: Bitmap) {
        // Convert to gray
        var grayMat = Mat(bmp.height, bmp.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bmp,grayMat)
        Imgproc.cvtColor(grayMat,grayMat, Imgproc.COLOR_BGR2GRAY)

        // Out mat
        out = Mat(grayMat.height(), grayMat.width(), CvType.CV_8UC4)
        Utils.bitmapToMat(bmp, out)

        //------------------------------------------------------------------------------------
        // Contours detection and circle detection
        //------------------------------------------------------------------------------------
        var threshold = Mat(grayMat.height(),grayMat.width(), CvType.CV_8UC4)
        Imgproc.threshold(grayMat, threshold,127.0,255.0, Imgproc.THRESH_BINARY_INV)
        /*Imgproc.adaptiveThreshold(grayMat,threshold,255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY_INV,11,2.0)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(6.0,6.0))
        Imgproc.morphologyEx(threshold,threshold,Imgproc.MORPH_DILATE,kernel)*/
        // Contours detection
        var contours:List<MatOfPoint> = ArrayList<MatOfPoint>()
        var hierarchy = Mat()
        var circles = LinkedList<DoubleArray>()
        var clockCenters = LinkedList<Point>()
        Imgproc.findContours(threshold,contours,hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        // Circles detection, has at least one circle been found?
        if(Tools.findCirclesInContours(contours,circles,MIN_CONTOURS_AREA)){
            // Get the circles that are most likely to be clock centers
            findClockCenter(out!!,circles,clockCenters)
        }else{
            Toast.makeText(this,"No circles. Try again.",Toast.LENGTH_LONG).show()
        }

        //Draw the circles
        var centerMissing = 0
        for(i in 0 until clockCenters.size){
            if(clockCenters.get(i)!=null){
                Imgproc.circle(out,clockCenters?.get(i),5,Scalar(0.0,255.0,0.0),5)
            }else{
                centerMissing++
            }
        }
        Toast.makeText(this,"Missing center: "+centerMissing,Toast.LENGTH_LONG).show()


        //------------------------------------------------------------------------------------
        // Display the result
        //------------------------------------------------------------------------------------
        Utils.matToBitmap(out, bmp)
        ivPicture!!.setImageBitmap(bmp)
        nbrContours!!.setText("Contours detected: "+contours.size)
        nbrCircles!!.setText("Circles detected: "+circles.size)
    }


    //------------------------------------------------------------------------------------
    // From the given list of circles, get the ones that are most likely to be clock
    // centers.
    // For that we create approximated centers in function of the number of rows and columns
    // of the matrice and then, get the closest circles to these approximated centers.
    //------------------------------------------------------------------------------------
    fun findClockCenter(src: Mat, inList: LinkedList<DoubleArray>, centers: LinkedList<Point>){
        val centerGap = src.height()/(MATRICE_ROW).toDouble()  // Supposed gap between two center
        // From the number of col and row of the matrice, approximate the positions of the centers
        for(row in 0 until MATRICE_ROW){
            for(col in 0 until MATRICE_COL){
                var distX = col*centerGap + centerGap/2
                var distY = row*centerGap + centerGap/2
                var center = Point(distX,distY)
                Imgproc.circle(out,center,20,
                    Scalar(0.0,0.0,255.0), 20)
                // Get the closest circle from this center, if minDist is not modified it means
                // that no circle has been detected for this center.
                var minDist = centerGap/3
                var bestCenter: Point? = null
                for(i in 0 until inList.size) {
                    var tempDist = Tools.distance2Points(center,
                        Point(inList.get(i)[0], inList.get(i)[1]))
                    if (tempDist < minDist) {
                        minDist = tempDist
                        bestCenter = Point(inList.get(i)[0], inList.get(i)[1])
                    }
                }

                if(bestCenter!=null) {
                    centers.addLast(bestCenter)
                }else{
                    //No circle near the approximated center
                    centers.addLast(null)
                }
            }
        }
    }
}
