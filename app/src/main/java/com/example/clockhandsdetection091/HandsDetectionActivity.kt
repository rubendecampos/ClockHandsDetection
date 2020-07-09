package com.example.clockhandsdetection091

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*


class HandsDetectionActivity : AppCompatActivity() {

    val TAG = "HandsDetectionActivity"
    val MIN_CONTOURS_AREA = 3
    val HAND_ANGLE_TOLERANCE = 5
    var MATRICE_ROW = 3
    var MATRICE_COL = 2

    lateinit var ivPicture: ImageView
    lateinit var txtView1: TextView
    lateinit var txtView2: TextView

    // variables
    var bmpMatrice: Bitmap? = null
    var matriceMat: Mat? = null
    var clockCenters = LinkedList<Point>()
    val matList: MutableList<Mat?> = mutableListOf()
    var clockArray: Clocks? = null
    var anglesString: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hands_detection)

        // Find all the view by ID
        val btnCompute: Button = findViewById(R.id.btnCompute)
        val btnCalibrate: Button = findViewById(R.id.btnCalibrate)
        txtView1 = findViewById(R.id.txt1)
        txtView2 = findViewById(R.id.txt2)
        ivPicture = findViewById(R.id.ivPicture)

        // Get the Intent extra parameters
        val intent = intent
        MATRICE_ROW = intent.getStringExtra("nbrRow").toInt()
        MATRICE_COL = intent.getStringExtra("nbrCol").toInt()
        val filePath = intent.getStringExtra("filePath")
        val bmp = BitmapFactory.decodeFile(filePath)
        clockArray = Clocks(MATRICE_COL*MATRICE_ROW)

        // Find the contour of the matrice and apply a warpPerspective on it.
        bmpMatrice = Tools.transformRectPerspective(bmp,
            MATRICE_ROW/MATRICE_COL.toDouble())
        ivPicture.setImageBitmap(bmpMatrice)

        // On the btnCompute click, start the detection
        btnCompute.setOnClickListener{
            detectCenters()
            detectHandsPosition()
        }

        // On the btnAngle click, send the calculated angles (in demo mode, display the angles
        // in a scroll view)
        btnCalibrate.setOnClickListener{
            var sendAngle = Intent(this,CommunicationActivity::class.java)
            sendAngle.putExtra("calibrate",anglesString)
            startActivity(sendAngle)
        }
    }

    //------------------------------------------------------------------------------------
    // Center detection. Detect each clocks centers to, then detect the hands positions.
    //------------------------------------------------------------------------------------
    fun detectCenters() {
        // Convert to gray
        val grayMat = Mat(bmpMatrice!!.height, bmpMatrice!!.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bmpMatrice,grayMat)
        Imgproc.cvtColor(grayMat,grayMat, Imgproc.COLOR_BGR2GRAY)

        // Convert the bitmap picture to Mat
        matriceMat = Mat(grayMat.height(), grayMat.width(), CvType.CV_8UC4)
        Utils.bitmapToMat(bmpMatrice, matriceMat)

        // Apply a Canny edge detection.
        val threshold = Mat(grayMat.height(),grayMat.width(), CvType.CV_8UC4)
        //Imgproc.GaussianBlur(grayMat,threshold,Size(5.0,5.0),0.0)
        //Imgproc.Canny(threshold,threshold,10.0,100.0,3)
        Imgproc.threshold(grayMat, threshold,127.0,255.0, Imgproc.THRESH_BINARY_INV)
        /*Imgproc.adaptiveThreshold(grayMat,threshold,255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY_INV,11,2.0)*/
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0,3.0))
        Imgproc.morphologyEx(threshold,threshold,Imgproc.MORPH_CLOSE,kernel)
        // Contours detection
        val contours:List<MatOfPoint> = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        val circles = LinkedList<DoubleArray>()
        Imgproc.findContours(threshold,contours,hierarchy, Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE)

        // Circles detection. Has at least one circle been found?
        if(Tools.findCirclesInContours(contours,circles,MIN_CONTOURS_AREA)){
            // Get the circles that are most likely to be clock centers
            clockCenters = findClockCenter(matriceMat!!, circles)
        }else{
            Toast.makeText(this,"No circles. Try again.",Toast.LENGTH_LONG).show()
        }

        /*for(i in 0 until contours.size){
            Imgproc.drawContours(out,contours,i, Scalar(255.0,0.0,0.0),1)
        }*/

        // Assuming that the centers detected aren't wrong, we want the distance between the
        // centers, that'll be the size of our clocks.
        // So we check every existing center from our list and calculate its distance with the
        // next center. The minimum distance'll be chose as the clock size.
        var centerMissing = 0
        var clockSize = matriceMat!!.width().toDouble()
        for(i in 0 until clockCenters.size-1){
            if(clockCenters.get(i) != null && clockCenters.get(i+1) != null){
                var temp = Tools.distance2Points(clockCenters.get(i),clockCenters.get(i+1))
                if(temp < clockSize){
                    clockSize = temp
                }
            }
        }

        // Now we crop each clocks and put them in a list of Mat. For the cropping, we use
        // rectangles centered in each centers position and with a size of "clockSize".
        val out = Mat()                 // I copy the matriceMat to a new mat to get a picture
        matriceMat!!.copyTo(out)        // without all the lines and circles drawn in it.
        for(i in 0 until clockCenters.size){
            if(clockCenters.get(i)!=null){
                Imgproc.circle(out,clockCenters.get(i),5,Scalar(0.0,255.0,0.0),10)
                val pt1 = Point(clockCenters.get(i).x-clockSize/2,
                    clockCenters.get(i).y-clockSize/2)
                val pt2 = Point(pt1.x+clockSize, pt1.y+clockSize)
                val roi = Rect(pt1,pt2)
                // Cropping the image of the clock, then calculate its angles
                matList.add(i,matriceMat!!.submat(roi))
                Imgproc.rectangle(out,roi,Scalar(0.0,0.0,255.0),2)
            }else{
                centerMissing++
                matList.add(i,null)
            }
        }
        Toast.makeText(this,"Missing center: "+centerMissing,Toast.LENGTH_LONG).show()

        //------------------------------------------------------------------------------------
        // Display the result
        //------------------------------------------------------------------------------------
        val bmp = Bitmap.createBitmap(bmpMatrice!!)
        Utils.matToBitmap(out, bmp)
        ivPicture.setImageBitmap(bmp)
        txtView1.setText("Contours detected: "+contours.size)
        txtView2.setText("Circles detected: "+circles.size)
    }


    //------------------------------------------------------------------------------------
    // From the given list of circles, get the ones that are most likely to be clock
    // centers.
    // For that we create approximated centers in function of the number of rows and columns
    // of the matrice and then, get the closest circles to these approximated centers.
    //------------------------------------------------------------------------------------
    fun findClockCenter(src: Mat, inList: LinkedList<DoubleArray>): LinkedList<Point>{
        val centerGap = src.height()/(MATRICE_ROW).toDouble()  // Supposed gap between two center
        val centers = LinkedList<Point>()

        // From the number of col and row of the matrice, approximate the positions of the centers
        for(row in 0 until MATRICE_ROW){
            for(col in 0 until MATRICE_COL){
                val distX = col*centerGap + centerGap/2
                val distY = row*centerGap + centerGap/2
                val center = Point(distX,distY)
                //Imgproc.circle(out,center,20,
                    //Scalar(0.0,0.0,255.0), 20)
                // Get the closest circle from this center, if minDist is not modified it means
                // that no circle has been detected for this center.
                var minDist = centerGap/3
                var bestCenter: Point? = null
                for(i in 0 until inList.size) {
                    val tempDist = Tools.distance2Points(center,
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

        return centers
    }

    //------------------------------------------------------------------------------------
    // Apply the Hough line detection, to detect and calculate the angles of the clock
    // hands.
    //------------------------------------------------------------------------------------
    fun detectHandsPosition(){
        val out = Mat()
        matriceMat!!.copyTo(out)
        anglesString = ""
        var nbrLines = 0
        var nbrMergedLines = 0

        for(clockIndex in 0 until matList.size){
            if(matList[clockIndex] != null){
                val clock = matList[clockIndex]
                val center = Point(clock!!.width()/2.0,clock.height()/2.0)

                // Convert to gray
                val grayMat = Mat(clock.size(),CvType.CV_8UC4)
                Imgproc.cvtColor(clock, grayMat, Imgproc.COLOR_BGR2GRAY)

                // Apply a Canny edge detection.
                val canny = Mat(grayMat.height(),grayMat.width(), CvType.CV_8UC4)
                Imgproc.GaussianBlur(grayMat,canny,Size(5.0,5.0),0.0)
                Imgproc.Canny(canny,canny,10.0,100.0,3)

                // Line detection using Hough transformation
                val matOfLines = Mat()
                Imgproc.HoughLinesP(canny,matOfLines,1.0,Math.PI/180,
                    50,10.0,100.0)

                // List of all the lines.
                val linesList = LinkedList<Line>()
                // Get all the lines from the mat of lines
                for (i in 0 until matOfLines.rows()) {
                    val line = Line()
                    line.setLine(matOfLines.get(i,0)) // set the line with a DoubleArray
                    linesList.addLast(line)
                }

                // Remove the bad lines (lines with an angle significantly different from the angle
                // with the center)
                val goodLines = Tools.removeBadLines(linesList, center)

                // Merge the lines with the same linear equation, in the best case it should
                // return 4 lines.
                val mergedLines = Tools.mergeLines(goodLines,0.5,clock.width()/20.0)

                //Sort the lines by their length and get only the first 4 lines (or less)
                fun selector(id: Line): Double = Tools.lengthLine(id)
                val sortedLines = mergedLines.sortedByDescending{ selector(it) }
                val biggestLine: List<Line>
                if(sortedLines.size>4){
                    biggestLine = sortedLines.subList(0,4)
                }else{
                    biggestLine = sortedLines
                }

                // Draw the lines
                for(i in 0 until mergedLines.size){
                    // Calculate the exact position of the points in the full picture.
                    val pt1 = Point(mergedLines[i].p1.x+ clockCenters[clockIndex].x - center.x,
                        mergedLines[i].p1.y+ clockCenters[clockIndex].y - center.y)
                    val pt2 = Point(mergedLines[i].p2.x+ clockCenters[clockIndex].x - center.x,
                        mergedLines[i].p2.y+ clockCenters[clockIndex].y - center.y)

                    Imgproc.line(out, pt1, pt2, Scalar(255.0, 0.0, 0.0), 2)
                }



                // Calculate the angle, update the list of clock and add it to the string
                //clockArray!!.clocks[clockIndex].angle1 = Tools.angleClockwise(center,furthestPoints[0]).toInt()
                //clockArray!!.clocks[clockIndex].angle2 =  Tools.angleClockwise(center,furthestPoints[1]).toInt()
                anglesString += "Clock " + clockIndex + ": " + clockArray!!.clocks[clockIndex].angle1 +
                        " / " + clockArray!!.clocks[clockIndex].angle2 + "\n"
                nbrLines += linesList.size
                nbrMergedLines += mergedLines.size
            }else{
                anglesString += "Clock " + clockIndex + ": " + "not detected\n"
            }
        }

        // Display the result
        val bmp = Bitmap.createBitmap(bmpMatrice!!)
        Utils.matToBitmap(out, bmp)
        ivPicture.setImageBitmap(bmp)
        txtView1.setText("Lines detected: "+nbrLines)
        txtView2.setText("Merged lines detected: "+nbrMergedLines)
    }

    //------------------------------------------------------------------------------------
    // Create the option menu. Instead of computing everything at once, the menu allow the
    // user to compute each part individually.
    //------------------------------------------------------------------------------------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.detection_menu,menu)
        return true
    }

    //------------------------------------------------------------------------------------
    // On each options Item selected in the menu, do something.
    //------------------------------------------------------------------------------------
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var id:Int? = item.itemId

        if(id == R.id.action_detect_centers){
            detectCenters()
            return true
        }else if(id == R.id.action_detect_hands){
            detectHandsPosition()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}
