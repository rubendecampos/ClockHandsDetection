package com.example.clockhandsdetection091

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*


class HandsDetectionActivity : AppCompatActivity() {

    val TAG = "HandsDetectionActivity"
    val MIN_CONTOURS_AREA = 10
    val MATRICE_ROW = 8
    val MATRICE_COL = 3

    var ivPicture: ImageView? = null
    var nbrContours: TextView? = null
    var nbrCircles: TextView? = null
    var bmp: Bitmap? = null
    var out: Mat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hands_detection)

        var btnCaptureAgain: Button = findViewById(R.id.btnCaptureAgain)
        var btnCompute: Button = findViewById(R.id.btnCompute)
        nbrContours = findViewById(R.id.nbrContours)
        nbrCircles = findViewById(R.id.nbrCircles)
        ivPicture = findViewById(R.id.ivPicture)
        val intent = intent
        val filePath = intent.getStringExtra("filePath")
        bmp = BitmapFactory.decodeFile(filePath)
        ivPicture!!.setImageBitmap(bmp)

        // On the btnCaptureAgain click, start the MainActivity again
        btnCaptureAgain.setOnClickListener({
            val newIntent = Intent(this,MainActivity::class.java)
            startActivity(newIntent)
        })

        // On the btnCompute click, start the detection
        btnCompute.setOnClickListener({
            detection()
        })
    }

    //------------------------------------------------------------------------------------
    // Complete code of the clock hands detection
    //------------------------------------------------------------------------------------
    fun detection() {
        // Convert to gray
        var grayMat = Mat(bmp!!.height, bmp!!.width, CvType.CV_8UC4)
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
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0,5.0))
        Imgproc.morphologyEx(threshold,threshold,Imgproc.MORPH_OPEN,kernel)*/

        var contours:List<MatOfPoint> = ArrayList<MatOfPoint>()
        var hierarchy = Mat()
        var circles = LinkedList<DoubleArray>()
        var clockCenters = LinkedList<DoubleArray>()
        Imgproc.findContours(threshold,contours,hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        findCirclesInContours(contours,circles)
        // Get the circles that are most likely to be clock centers
        findClockCenter(out!!,circles,clockCenters)

        /*val src = Mat(4, 1, CvType.CV_32FC2)
        val p1 = IntArray(2,init = {clockCenters.get(0)[0].toInt();clockCenters.get(0)[1].toInt()})
        val p2 = IntArray(2,init = {clockCenters.get(2)[0].toInt();clockCenters.get(2)[1].toInt()})
        val p3 = IntArray(2,init = {clockCenters.get(21)[0].toInt();clockCenters.get(21)[1].toInt()})
        val p4 = IntArray(2,init = {clockCenters.get(23)[0].toInt();clockCenters.get(23)[1].toInt()})
        src.put(0,0,p1)
        src.put(1,0,p2)
        src.put(2,0,p3)
        src.put(3,0,p4)
        val dst = Mat(4, 1, CvType.CV_32FC2)
        dst.put(0,0,IntArray(2,init = {0;0}))
        dst.put(1,0,IntArray(2,init = {0;out!!.width()}))
        dst.put(2,0,IntArray(2,init = {out!!.height();0}))
        dst.put(3,0,IntArray(2,init = {out!!.height();out!!.width()}))

        val perspectiveTransform = Imgproc.getPerspectiveTransform(src, dst)
        Imgproc.warpPerspective(out,out,perspectiveTransform,out!!.size())*/

        //------------------------------------------------------------------------------------
        // Warping the picture to get only the matrice of clock
        //------------------------------------------------------------------------------------

        //------------------------------------------------------------------------------------
        // Display the result
        //------------------------------------------------------------------------------------
        Utils.matToBitmap(out, bmp)
        ivPicture!!.setImageBitmap(bmp)
        nbrContours!!.setText("Contours detected: "+contours.size)
        nbrCircles!!.setText("Circles detected: "+circles.size)
    }

    //------------------------------------------------------------------------------------
    // Find all circle in the contour list by comparing their area with the area of their
    // enclosing circle.
    // When a circle is found, another circle with a really close center isn't taken.
    //------------------------------------------------------------------------------------
    private fun findCirclesInContours(contours:List<MatOfPoint>, circles: LinkedList<DoubleArray>): Boolean{
        var radius = FloatArray(1)
        var center = Point()
        for(i in 0 until contours.size){
            if(Imgproc.contourArea(contours[i]) > MIN_CONTOURS_AREA) {
                //Get i-th contour and get his enclosing circle
                var contour = MatOfPoint2f()
                contours[i].convertTo(contour, CvType.CV_32F)
                Imgproc.minEnclosingCircle(contour, center, radius)

                //Calculate the area of the enclosing circle and get the actual area of the contour
                val area = Math.PI * radius[0] * radius[0]
                val contourArea = Imgproc.contourArea(contour)

                //Compare the area of the enclosing circle with the contour
                //If they are similar, the contour is a circle
                if (contourArea < area &&
                    contourArea > area * 0.7
                ) {
                    val circle = DoubleArray(3)
                    circle.set(0, center.x)
                    circle.set(1, center.y)
                    circle.set(2, radius[0].toDouble())

                    //is the center close to one a the founded circles center?
                    var isClose = false
                    /*for (i in 0 until circles.size) {
                        if (circles[i][0] - circles[i][2] < circle[0] &&
                            circles[i][0] + circles[i][2] > circle[0] &&
                            circles[i][1] - circles[i][2] < circle[1] &&
                            circles[i][1] + circles[i][2] > circle[1]
                        ) {
                            isClose = true
                        }
                    }*/

                    //If it is not close, add the circle to the list and draw it
                    if (!isClose) {
                        circles.addLast(circle)
                    }
                } else {
                    Imgproc.drawContours(out, contours, i, Scalar(255.0, 0.0, 0.0), 2)
                }
            }
        }

        return circles.size>0
    }

    //------------------------------------------------------------------------------------
    // From the given list of circles, get the ones that are most likely to be clock
    // centers.
    // For that we draw a virtual representation of where the matrice should be with its
    // clock centers, then get the closest circles to these centers.
    //------------------------------------------------------------------------------------
    fun findClockCenter(dst: Mat,inList: LinkedList<DoubleArray>,outList: LinkedList<DoubleArray>){
        // Draw the rectangle
        val centerGap = dst.height()/(MATRICE_ROW).toDouble()  // Supposed gap between two center
        val matriceWidth = centerGap*MATRICE_COL
        val margeX = (dst.width()-matriceWidth)/2
        //Imgproc.rectangle(out,rect,Scalar(0.0,0.0,255.0),20)
        // From the number of col and row of the matrice, draw all the clock center circles
        // in the rectangle
        val circlesMatrice = LinkedList<Point>()
        for(row in 0 until MATRICE_ROW){
            for(col in 0 until MATRICE_COL){

                var distX = col*centerGap + centerGap/2 + margeX
                var distY = row*centerGap + centerGap/2
                var center = Point(distX,distY)
                circlesMatrice.addLast(center)
                Imgproc.circle(out,center,20,
                    Scalar(0.0,0.0,255.0), 20)
                // Get the closest circle from this center
                var minDist = matriceWidth
                var bestCenter: DoubleArray? = null
                for(i in 0 until inList.size) {
                    var tempDist = distance2Points(center,
                        Point(inList.get(i)[0], inList.get(i)[1]))
                    if (tempDist < minDist) {
                        minDist = tempDist
                        bestCenter = inList.get(i)
                    }
                }
                outList.addLast(bestCenter)
                Imgproc.circle(out, Point(bestCenter!![0], bestCenter[1]), bestCenter[2].toInt(),
                    Scalar(0.0, 255.0, 0.0), 2)
            }
        }
    }

    //------------------------------------------------------------------------------------
    // Return the distance between 2 points
    //------------------------------------------------------------------------------------
    fun distance2Points(pt1:Point, pt2:Point): Double{
        return Math.sqrt(Math.pow((pt2.x-pt1.x),2.0) + Math.pow((pt2.y-pt1.y),2.0))
    }
}
