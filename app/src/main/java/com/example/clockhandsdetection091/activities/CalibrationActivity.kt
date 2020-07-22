package com.example.clockhandsdetection091.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.clockhandsdetection091.*
import com.example.clockhandsdetection091.enumeration.Event
import com.example.clockhandsdetection091.utils.Calibration
import com.example.clockhandsdetection091.utils.Tools
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Calibration activity. Calibrate the matrice device in 3 steps.
 * 1. Take a picture of the device.
 * 2. Compute the picture to detect the clock's centers, then the clock's hands positions.
 * 3. Calibrate the device using the found positions.
 * Those steps need to be done multiple times to completely calibrate the device.
 * @property [bmpMatrice] bitmap of the matrice picture, displayed on screen.
 * @property [matriceMat] mat of the matrice picture.
 * @property [matList] mat list of each clock.
 * @property [clockArray] array of each clock and their params (center, state, angles,...).
 * @property [calibrate] used to calibrate each clock.
 * @property [currentPicturePath] the current path of the picture taken.
 * @property [isComputed] has the picture been computed?
 * @property [isPictureTaken] has the picture been taken?
 * @author Ruben De Campos
 */
class CalibrationActivity : AppCompatActivity() {

    val TAG = "HandsDetectionActivity"
    val MIN_CONTOURS_AREA = 3
    val REQUEST_IMAGE_CAPTURE = 1
    var MATRICE_ROW = 3
    var MATRICE_COL = 2

    lateinit var ivPicture: ImageView
    lateinit var txtView1: TextView
    lateinit var txtView2: TextView

    // variables
    private var bmpMatrice: Bitmap? = null
    private var matriceMat: Mat? = null
    private val matList: MutableList<Mat?> = mutableListOf()
    private var clockArray: Clocks? = null
    private var calibrate: Calibration? = null
    private var currentPicturePath: String = ""
    private var isComputed = false
    private var isPictureTaken = false

    /**
     * On the activity creation.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        // Find all the view by ID
        val btnCompute: Button = findViewById(R.id.btnCompute)
        val btnCalibrate: Button = findViewById(R.id.btnCalibrate)
        val btnPicture: Button = findViewById(R.id.btnPicture)
        txtView1 = findViewById(R.id.txt1)
        txtView2 = findViewById(R.id.txt2)
        ivPicture = findViewById(R.id.ivPicture)

        // Get the Intent extra parameters
        val intent = intent
        MATRICE_ROW = intent.getStringExtra("nbrRow").toInt()
        MATRICE_COL = intent.getStringExtra("nbrCol").toInt()
        val jsonString = intent.getStringExtra("jsonString")
        clockArray = Clocks(MATRICE_COL * MATRICE_ROW)
        calibrate = Calibration(clockArray!!.clocks.size,jsonString)

        // On the activity creation, take a first picture
        takePicture()

        // On the btnPicture click, take a picture
        btnPicture.setOnClickListener{
            takePicture()
        }

        // On the btnCompute click, start the detection
        btnCompute.setOnClickListener{
            if(isPictureTaken){
                centersDetection()
                handsDetection()
                isComputed = true
                isPictureTaken = false;
            }else{
                Toast.makeText(this,"Please take a picture first",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // On the btnAngle click, put in extra the detected angles and the action to do for the
        // matrice.
        btnCalibrate.setOnClickListener{
            if(isComputed){
                val actionJSON = calibrate!!.calibrate(clockArray!!)
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("clock array",clockArray.toString())
                intent.putExtra("action",actionJSON.toString())
                startActivity(intent)
                isComputed = false
            }else{
                Toast.makeText(this,"Please compute first",Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Center detection. Detect each clocks centers to, then detect the hands positions.
     */
    fun centersDetection() {
        matList.clear()

        // Convert to gray
        val grayMat = Mat(bmpMatrice!!.height, bmpMatrice!!.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bmpMatrice,grayMat)
        Imgproc.cvtColor(grayMat,grayMat, Imgproc.COLOR_BGR2GRAY)

        // Convert the bitmap picture to Mat
        matriceMat = Mat(grayMat.height(), grayMat.width(), CvType.CV_8UC4)
        Utils.bitmapToMat(bmpMatrice, matriceMat)

        // Apply a threshold filter
        val threshold = Mat(grayMat.height(),grayMat.width(), CvType.CV_8UC4)
        Imgproc.threshold(grayMat, threshold,127.0,255.0, Imgproc.THRESH_BINARY_INV)

        /*Imgproc.GaussianBlur(grayMat,threshold,Size(5.0,5.0),0.0)
        Imgproc.Canny(threshold,threshold,10.0,100.0,3)
        Imgproc.adaptiveThreshold(grayMat,threshold,255.0,
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
        var clockCenters = LinkedList<Point>()
        if(Tools.findCirclesInContours(contours, circles, MIN_CONTOURS_AREA)){
            // Get the circles that are most likely to be clock centers
            clockCenters = findClockCenter(matriceMat!!, circles)

            // Update the center attribute of each clock in clockArray
            for (i in clockCenters.indices) {
                if(clockCenters[i] != null)
                    clockArray!!.clocks[i].center = clockCenters[i]
            }
        }else{
            Toast.makeText(this,"No circles. Try again.",Toast.LENGTH_LONG).show()
        }

        // Assuming that the centers detected aren't wrong, we want the distance between the
        // centers, that'll be the size of our clocks.
        // So we check every existing center from our list and calculate its distance with the
        // next center. The minimum distance'll be chose as the clock size.
        var centerMissing = 0
        var clockSize = matriceMat!!.width().toDouble()
        for(i in 0 until clockCenters.size-1){
            if(clockCenters[i] != null && clockCenters[i+1] != null){
                val temp = Tools.distance2Points(clockCenters[i], clockCenters[i+1])
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
            if(clockCenters[i]!=null){
                Imgproc.circle(out,clockCenters[i],5,
                    Scalar(0.0,255.0,0.0), 10)
                val pt1 = Point(clockCenters[i].x-clockSize/2,
                    clockCenters[i].y-clockSize/2)
                val pt2 = Point(pt1.x+clockSize, pt1.y+clockSize)
                val roi = Rect(pt1,pt2)

                // If the rectangle isn't out of bound, crop the image of the clock.
                if(roi.x>0 && roi.y>0 && roi.x+roi.width<matriceMat!!.width()
                    && roi.y+roi.height < matriceMat!!.height())
                {
                    matList.add(i, matriceMat!!.submat(roi))
                    Imgproc.rectangle(out,roi,Scalar(0.0,0.0,255.0),2)
                }else{
                    matList.add(i,null)
                }
            }else{
                centerMissing++
                matList.add(i,null)
            }
        }
        Toast.makeText(this, "Missing center: $centerMissing",Toast.LENGTH_LONG).show()

        //------------------------------------------------------------------------------------
        // Display the result
        //------------------------------------------------------------------------------------
        val bmp = Bitmap.createBitmap(bmpMatrice!!)
        Utils.matToBitmap(out, bmp)
        ivPicture.setImageBitmap(bmp)
        txtView1.text = "Contours detected: "+contours.size
        txtView2.text = "Circles detected: "+circles.size
    }


    /**
     * From the given list of circles, get the ones that are most likely to be clock centers.
     * For that we create approximated centers in function of the number of rows and columns
     * of the matrice and then, get the closest circles to these approximated centers.
     * @param [src] the mat object of the matrice.
     * @param [inList] input list of every circles found.
     * @return the list of points corresponding to each center.
     */
    fun findClockCenter(src: Mat, inList: LinkedList<DoubleArray>): LinkedList<Point>{
        val centerGap = src.height()/(MATRICE_ROW).toDouble()  // Supposed gap between two center
        val centers = LinkedList<Point>()

        // From the number of col and row of the matrice, approximate the positions of the centers
        for(row in 0 until MATRICE_ROW){
            for(col in 0 until MATRICE_COL){
                val distX = col*centerGap + centerGap/2
                val distY = row*centerGap + centerGap/2
                val center = Point(distX,distY)

                // Get the closest circle from this center, if minDist is not modified it means
                // that no circle has been detected for this center.
                var minDist = centerGap/3
                var bestCenter: Point? = null
                for(i in 0 until inList.size) {
                    val tempDist = Tools.distance2Points(center, Point(inList[i][0], inList[i][1]))
                    if (tempDist < minDist) {
                        minDist = tempDist
                        bestCenter = Point(inList.get(i)[0], inList[i][1])
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

    /**
     * Apply the Hough line detection, to detect and calculate the angles of the clock hands.
     */
    fun handsDetection(){
        val out = Mat()
        matriceMat!!.copyTo(out)
        var nbrLines = 0
        var nbrMergedLines = 0

        for(clockIndex in 0 until matList.size){
            if(matList[clockIndex] != null){
                val clockMat = matList[clockIndex]
                val center = Point(clockMat!!.width()/2.0,clockMat.height()/2.0)
                val clock = clockArray!!.clocks[clockIndex]

                // Convert to gray
                val grayMat = Mat(clockMat.size(),CvType.CV_8UC4)
                Imgproc.cvtColor(clockMat, grayMat, Imgproc.COLOR_BGR2GRAY)

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
                val mergedLines = Tools.mergeLines(goodLines, 0.5,
                    clockMat.width() / 20.0)

                // Draw the lines
                for(i in 0 until mergedLines.size){
                    // Calculate the exact position of the points in the full picture.
                    val pt1 = Point(mergedLines[i].p1.x+ clock.center.x - center.x,
                        mergedLines[i].p1.y+ clock.center.y - center.y)
                    val pt2 = Point(mergedLines[i].p2.x+ clock.center.x - center.x,
                        mergedLines[i].p2.y+ clock.center.y - center.y)

                    Imgproc.line(out, pt1, pt2, Scalar(255.0, 0.0, 0.0), 2)
                }

                // Find the hands angles in the list of merged lines
                val handsLines = LinkedList<Line>()
                val angles = findHandsInLines(mergedLines,handsLines,center,
                    clockMat.width()/1.5,clockMat.width()/2.0)

                // Update the list of clocks with the angles found
                if(angles.size < 2){
                    calibrate!!.clockEvents[clockIndex] = Event.EvTooCLose
                }else{
                    clockArray!!.clocks[clockIndex].angle1 = angles[0].toInt()
                    clockArray!!.clocks[clockIndex].angle2 = angles[1].toInt()
                }


                // Draw the hands lines
                for(i in 0 until handsLines.size){
                    // Calculate the exact position of the points in the full picture.
                    val pt1 = Point(handsLines[i].p1.x+ clock.center.x - center.x,
                        handsLines[i].p1.y+ clock.center.y - center.y)
                    val pt2 = Point(handsLines[i].p2.x+ clock.center.x - center.x,
                        handsLines[i].p2.y+ clock.center.y - center.y)

                    Imgproc.line(out, pt1, pt2, Scalar(0.0, 255.0, 0.0), 2)
                }

                nbrLines += linesList.size
                nbrMergedLines += mergedLines.size
            }else{
                calibrate!!.clockEvents[clockIndex] = Event.EvNotDetected
            }
        }

        // Display the result
        val bmp = Bitmap.createBitmap(bmpMatrice!!)
        Utils.matToBitmap(out, bmp)
        ivPicture.setImageBitmap(bmp)
        txtView1.text = "Lines detected: "+nbrLines
        txtView2.text = "Merged lines detected: "+nbrMergedLines
    }

    /**
     * Called by the handsDetection method. This function find the hands angles in
     * a given list of lines by calculating the intersection between each lines, and if a
     * few condition are fulfilled, it's a tip of a hand and the angle is added to the list.
     * @param [inLines] input list of lines.
     * @param [outLines] output list of lines corresponding to the clock's hands.
     * @param [center] center point of this clock.
     * @param [minRadius] minimal radius, the intersections must within that range to the lines.
     * @param [bigRadius] big radius, the intersections must be out of that range to the center.
     * @return the list of each clock hand's angles.
     */
    fun findHandsInLines(inLines: LinkedList<Line>, outLines: LinkedList<Line>, center: Point,
                         minRadius: Double, bigRadius: Double): LinkedList<Double>
    {
        // Create a copy of the list and a list for the resulted angles
        val linesCopy = LinkedList<Line>(inLines)
        val angles = LinkedList<Double>()

        // The conditions to have a good intersection are:
        // 1) It must be out of the clock radius (bigRadius as reference).
        // 2) It must be close to the two externals points of the two lines (minRadius as reference).

        // In some case, the hands are straight and the the Hough detection find one big
        // line for two hands. So when a line is bigger than bigRadius, we split it in two lines.
        for(i in linesCopy.indices){
            if(Tools.lengthLine(linesCopy[i]) > bigRadius){
                val middlePoint = Point((linesCopy[i].p2.x-linesCopy[i].p1.x)/2.0 +
                        linesCopy[i].p1.x, (linesCopy[i].p2.y-linesCopy[i].p1.y)/2.0
                        + linesCopy[i].p1.y)
                val newLine = Line(linesCopy[i].p1,middlePoint)
                linesCopy.addLast(newLine)
                linesCopy[i].p1 = middlePoint
            }
        }

        // Iterate through all lines
        val it1 = linesCopy.iterator()
        while(it1.hasNext()){
            val line1 = it1.next()
            val it2 = linesCopy.iterator()
            while(it2.hasNext()){
                 val line2 = it2.next()
                // We cannot find the intersection of the same line
                if(line1 != line2){
                    // find the externals points
                    // P1
                    var externP1: Point = if(Tools.distance2Points(center, line1.p1) >
                        Tools.distance2Points(center, line1.p2))
                    {
                        line1.p1
                    }else{
                        line1.p2
                    }
                    // P2
                    var externP2: Point = if(Tools.distance2Points(center, line2.p1) >
                        Tools.distance2Points(center, line2.p2))
                    {
                        line2.p1
                    }else{
                        line2.p2
                    }

                    // Calculate the intersection between both lines
                    val intersection = Tools.intersection(line1, line2)

                    // Not parallel ?
                    if(intersection != null){
                        //----------------------------------------------------------
                        // Now we have to test if the condition are satisfied.
                        //----------------------------------------------------------
                        if(Tools.distance2Points(center, intersection) > bigRadius &&
                            Tools.distance2Points(intersection, externP1) < minRadius &&
                            Tools.distance2Points(intersection, externP2) < minRadius)
                        {
                            // The angle between this intersection and the center is added to the
                            // result list only if it's not similar to an already found angle.
                            val angle = Tools.angleClockwise(center, intersection)
                            var notSimilar = true
                            angles.forEach(){
                                if(angle < it+2 && angle > it-2)
                                    notSimilar = false
                            }
                            if(notSimilar && angles.size < 2){
                                angles.addLast(angle)
                                outLines.addLast(Line(center, intersection))
                            }
                        }
                    }
                }
            }
        }

        return angles
    }

    /**
     * Create the option menu. Instead of computing everything at once, the menu allow the
     * user to compute each part individually.
     * @param [menu]
     * @return
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.detection_menu,menu)
        return true
    }

    /**
     * On each options Item selected in the menu, do something.
     * @param [item] item selected.
     * @return
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id:Int? = item.itemId

        if(id == R.id.action_detect_centers){
            centersDetection()
            return true
        }else if(id == R.id.action_detect_hands){
            handsDetection()
            isComputed = true
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * On the activity result, apply the perspective transformation and display the picture.
     * @param [requestCode]
     * @param [resultCode]
     * @param [data]
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> if (resultCode == Activity.RESULT_OK) {
                // Find the contour of the matrice and apply a warpPerspective on it.
                val bmp = BitmapFactory.decodeFile(currentPicturePath)
                bmpMatrice = Tools.transformRectPerspective(bmp,
                    MATRICE_ROW / MATRICE_COL.toDouble())
                ivPicture.setImageBitmap(bmpMatrice)
                isPictureTaken = true;
            }
        }
    }

    /**
     * Start the ACTION_IMAGE_CAPTURE intent to take a picture.
     */
    private fun takePicture(){
        // Request the permission to use the camera if not already granted
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, Array<String>(1, init =
            {android.Manifest.permission.CAMERA}), 100)
        }

        // create and start the intent to take a picture
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val pictureFile = try{
            getOutputMediaFile()
        }catch(e: IOException){
            // Error occured while creating the File
            null
        }
        // Continue only if the File was created successfully
        pictureFile?.also {
            val pictureUri = FileProvider.getUriForFile(this,
                "com.example.android.fileprovider",it)
            intent.putExtra(MediaStore.EXTRA_OUTPUT,pictureUri)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    /**
     * Get the output file. Update the currentPicturePath property.
     * @return the file object.
     */
    private fun getOutputMediaFile(): File? {
        var state = Environment.getExternalStorageState()
        if(state.equals(Environment.MEDIA_MOUNTED)) {
            var storageDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString())
            // Create the File
            var outputFile = File(storageDir,
                "temp.jpg").apply()
            {
                // Save the path for use with ACTION_VIEW intents
                currentPicturePath = absolutePath
            }
            return outputFile
        }else{
            return null
        }
    }
}
