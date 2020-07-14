package com.example.clockhandsdetection091.utils

import android.graphics.Bitmap
import com.example.clockhandsdetection091.Line
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

//------------------------------------------------------------------------------------
// Class that provide many method to help for the clock hands detection, as
// findCirclesInContours(), distance2Points(), etc...
//------------------------------------------------------------------------------------
class Tools {
    companion object{
        //------------------------------------------------------------------------------------
        // Find all circle in the contour list by comparing their area with the area of their
        // enclosing circle. Contours too small aren't taken in consideration.
        //------------------------------------------------------------------------------------
        fun findCirclesInContours(contours:List<MatOfPoint>, circles: LinkedList<DoubleArray>,
                                  minArea: Int): Boolean{
            var radius = FloatArray(1)
            var center = Point()
            for(i in contours.indices){
                if(Imgproc.contourArea(contours[i]) > minArea) {
                    //Get i-th contour and get his enclosing circle
                    var contour = MatOfPoint2f()
                    contours[i].convertTo(contour, CvType.CV_32F)
                    Imgproc.minEnclosingCircle(contour, center, radius)

                    //Calculate the area of the enclosing circle and get the actual area
                    // of the contour
                    val circleArea = Math.PI * radius[0] * radius[0]
                    val contourArea = Imgproc.contourArea(contour)

                    //Compare the area of the enclosing circle with the contour
                    //If they are similar, the contour is a circle
                    if (contourArea < circleArea &&
                        contourArea > circleArea * 0.6
                    ) {
                        val circle = DoubleArray(3)
                        circle[0] = center.x
                        circle[1] = center.y
                        circle[2] = radius[0].toDouble()

                        circles.addLast(circle)
                    }
                }
            }
            return circles.size>0
        }


        //------------------------------------------------------------------------------------
        // Return the distance between 2 points
        //------------------------------------------------------------------------------------
        fun distance2Points(pt1:Point, pt2:Point): Double{
            return sqrt((pt2.x - pt1.x).pow(2.0) + (pt2.y - pt1.y).pow(2.0))
        }

        //------------------------------------------------------------------------------------
        // Return the angle between two hands.
        //------------------------------------------------------------------------------------
        fun handsAngle(hand1: Int, hand2: Int): Int{
            var angle = abs(hand1 - hand2)
            // Calculate the smaller angle
            if(angle > 180){
                angle = 360-angle
            }

            return angle
        }

        //------------------------------------------------------------------------------------
        // Return the length of a line
        //------------------------------------------------------------------------------------
        fun lengthLine(line: Line): Double{
            return sqrt((line.p2.x - line.p1.x).pow(2.0) + (line.p2.y - line.p1.y).pow(2.0))
        }

        //------------------------------------------------------------------------------------
        // Calculate the angle of a line (0 is when the line goes straight up like below)
        //                              ^
        //                              |
        //                              |
        //                              |
        //                              o
        //------------------------------------------------------------------------------------
        fun angleClockwise(center: Point, edge: Point): Double{
            //Calculate the angle for the vector center-edge
            val theta = atan2(edge.y-center.y,edge.x-center.x)
            var angle = theta*(180.0/3.141592)
            //atan2 give an angle between -180 and 180, we convert that
            if(angle<0) angle += 360.0
            //Then, because the [0;0] is in the top left corner of the screen
            //we readjust the angle (our 0° angle is at the vertical)
            angle = (angle + 90)%360

            return angle
        }

        //------------------------------------------------------------------------------------
        // Find the biggest rectangle in the picture, get its perspective and warp it into
        // a new bitmap with the same height and a width given by the proportion.
        //------------------------------------------------------------------------------------
        fun transformRectPerspective(src: Bitmap, proportion: Double): Bitmap {
            // Convert to gray
            val grayMat = Mat(src.height, src.width, CvType.CV_8UC4)
            Utils.bitmapToMat(src,grayMat)
            Imgproc.cvtColor(grayMat,grayMat, Imgproc.COLOR_BGR2GRAY)

            // imageMat
            val imageMat = Mat(grayMat.height(), grayMat.width(), CvType.CV_8UC4)
            Utils.bitmapToMat(src, imageMat)

            //Apply a threshold, dilate it and find the contours
            val canny = Mat(grayMat.height(),grayMat.width(), CvType.CV_8UC4)
            //Imgproc.threshold(grayMat, threshold,127.0,255.0, Imgproc.THRESH_BINARY_INV)
            Imgproc.Canny(grayMat,canny,10.0,100.0)
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                Size(6.0,6.0))
            Imgproc.morphologyEx(canny,canny,Imgproc.MORPH_DILATE,kernel)

            val contours:List<MatOfPoint> = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(canny,contours,hierarchy, Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE)

            //Get the biggest contour
            var area = 0.0
            var index = 0
            for(i in contours.indices){
                val tempArea = Imgproc.contourArea(contours.get(i))
                if(tempArea > area){
                    area = tempArea
                    index = i
                }
            }

            // Convert contours(index) from MatOfPoint to MatOfPoint2f and approximate the
            // contour as rectangle
            val contour2f = MatOfPoint2f(*contours[index].toArray())
            val approxRect = MatOfPoint2f()
            val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
            Imgproc.approxPolyDP(contour2f, approxRect, approxDistance, true)

            // Get the 4 corners points and put them in a Mat var, they need to be ordered
            // correctly.
            // To ordered them, I check the two point in the upper part of the picture and set them
            // as topLeft and topRight, same for the lower part.
            val corners = Mat(4,1,CvType.CV_32FC2)
            // Upper part
            var topLeft = Point(imageMat.width().toDouble(),imageMat.height().toDouble())
            var topRight = Point(0.0,imageMat.height().toDouble())
            for(i in 0 until approxRect.rows()){
                val pt = Point(approxRect.get(i,0))
                if(pt.y < imageMat.height()/2){
                    if(pt.x > topRight.x){
                        topRight = pt
                    }
                    if(pt.x < topLeft.x){
                        topLeft = pt
                    }
                }
            }
            // Lower part
            var botLeft = Point(imageMat.width().toDouble(),0.0)
            var botRight = Point(0.0,0.0)
            for(i in 0 until approxRect.rows()){
                val pt = Point(approxRect.get(i,0))
                if(pt.y > imageMat.height()/2){
                    if(pt.x > botRight.x){
                        botRight = pt
                    }
                    if(pt.x < botLeft.x){
                        botLeft = pt
                    }
                }
            }

            corners.put(0,0,topLeft.x,topLeft.y)
            corners.put(1,0,topRight.x,topRight.y)
            corners.put(2,0,botLeft.x,botLeft.y)
            corners.put(3,0,botRight.x,botRight.y)
            // And create the 4 corners points of the future image that has the proportion of the
            // matrice.
            val newCorners = Mat(4,1,CvType.CV_32FC2)
            val matriceSize = Size(imageMat.width().toDouble(),
                imageMat.width().toDouble()*proportion)
            newCorners.put(0,0,0.0,0.0)
            newCorners.put(1,0,matriceSize.width,0.0)
            newCorners.put(2,0,0.0,matriceSize.height)
            newCorners.put(3,0,matriceSize.width,matriceSize.height)

            // Get the perspective transformation and warp it in matriceMat
            val matriceMat = Mat(matriceSize,CvType.CV_8UC4)
            val perspectiveTransform = Imgproc.getPerspectiveTransform(corners,newCorners)
            Imgproc.warpPerspective(imageMat,matriceMat,perspectiveTransform,matriceSize)

            // Create the destination Bitmap and configure it with the matriceMat size
            val dst = Bitmap.createBitmap(matriceMat.width(),matriceMat.height(), Bitmap.Config.RGB_565)
            Utils.matToBitmap(matriceMat, dst)
            return dst
        }

        //------------------------------------------------------------------------------------
        // Remove the bad lines from the list.
        // A bad line is a line where the angle between the line itself and the line
        // from the center to the furthest point is superior to a certain value
        //------------------------------------------------------------------------------------
        fun removeBadLines(lines: List<Line>, center:Point): LinkedList<Line>{
            val resultList = LinkedList<Line>()

            for(i in lines.indices){
                val pt1 = lines[i].p1
                val pt2 = lines[i].p2
                var angleCenter = 0.0
                var angleLine = 0.0

                //which point is the furthest from the center?
                if(distance2Points(center, pt1) > distance2Points(center, pt2)){
                    angleLine = angleClockwise(pt2, pt1)
                    angleCenter = angleClockwise(center, pt1)
                }else{
                    angleLine = angleClockwise(pt1, pt2)
                    angleCenter = angleClockwise(center, pt2)
                }

                //Calculate the difference
                //If the difference  is greater than 180°, we do an
                //addition instead of a subtraction
                var diff = abs(angleCenter-angleLine)
                if(diff > 180.0){
                    if(angleCenter>angleLine){
                        diff = 360-angleCenter + angleLine
                    }else{
                        diff = 360-angleLine + angleCenter
                    }
                }

                //difference lower than 20° -> good line!
                if(diff < 20.0){
                    resultList.addLast(lines[i])
                }
            }

            return resultList
        }

        //------------------------------------------------------------------------------------
        // Merge all lines close by and with similar angle to get only one big line representing
        // the hand edge.
        //------------------------------------------------------------------------------------
        fun mergeLines(lines: List<Line>, angleTolerance: Double, distTolerance: Double)
                : LinkedList<Line>{
            val mergedLines = LinkedList<Line>()

            for(lineIndex in lines.indices){
                // First we calculate the angle of the line
                val p1 = lines[lineIndex].p1
                val p2 = lines[lineIndex].p2
                // The angle range is from 0 to 180 and not 360
                var angle: Double
                if(p2.x < p1.x){
                    angle =
                        angleClockwise(p2, p1)
                }else{
                    angle = angleClockwise(p1, p2)
                }

                // First line? goes directly in mergedLines list
                if(mergedLines.size == 0){
                    mergedLines.add(lines[lineIndex])
                }else{
                    // Now we check if it match an angle from a mergedLine and if both lines got two
                    // close by points.
                    var merged = false
                    for(mergeIndex in mergedLines.indices){
                        // Calculate the merged line angle
                        val p1Merged = mergedLines[mergeIndex].p1
                        val p2Merged = mergedLines[mergeIndex].p2
                        var angleMerged = 0.0
                        if(p2Merged.x < p1Merged.x){
                            angleMerged = angleClockwise(p2Merged, p1Merged)
                        }else{
                            angleMerged = angleClockwise(p1Merged, p2Merged)
                        }

                        // Check if the angles match and if this lines is not already merged
                        if(angle > angleMerged-angleTolerance &&
                            angle < angleMerged+angleTolerance && !merged){

                            // Now check if two points from both lines are close by, and if the
                            // other two points are far away (at least the length of both lines).
                            var minMergedDist = lengthLine(mergedLines[mergeIndex]) +
                                    lengthLine(lines[lineIndex]) - distTolerance
                            if(distance2Points(p1, p1Merged) < distTolerance){
                                if(distance2Points(p2, p2Merged) > minMergedDist){
                                    mergedLines[mergeIndex] = Line(p2, p2Merged)
                                    merged = true
                                }
                            }else if(distance2Points(p1, p2Merged) < distTolerance){
                                if(distance2Points(p2, p1Merged) > minMergedDist){ mergedLines[mergeIndex] = Line(p2, p1Merged)
                                    merged = true
                                }
                            }else if(distance2Points(p2, p1Merged) < distTolerance){
                                if(distance2Points(p1, p2Merged) > minMergedDist){
                                    mergedLines[mergeIndex] = Line(p1, p2Merged)
                                    merged = true
                                }
                            }else if(distance2Points(p2, p2Merged) < distTolerance){
                                if(distance2Points(p1, p1Merged) > minMergedDist){
                                    mergedLines[mergeIndex] = Line(p1, p1Merged)
                                    merged = true
                                }
                            }
                        }
                    }
                    // If not merged, add this line to the merged list as a new merged line.
                    if(!merged){
                        mergedLines.addLast(lines[lineIndex])
                    }
                }
            }

            return mergedLines
        }

        //------------------------------------------------------------------------------------
        // Find the intersection of two lines by calculating their linear function and
        // calculating the intersection point.
        // If parallel lines, return null
        //------------------------------------------------------------------------------------
        fun intersection(line1: Line, line2: Line): Point?{
            //------------------------------------------------------
            // Equation for line1 -> y = ax + b
            //------------------------------------------------------
            var a: Double
            var b: Double
            // Is the curb ascending?
            if((line1.p2.y-line1.p1.y)*(line1.p2.x-line1.p1.x) > 0){
                a = 1.0     // ascending
            }else{
                a = -1.0    // descending
            }
            // deltaX must not be 0
            var deltaX = abs(line1.p2.x-line1.p1.x)
            if(deltaX == 0.0)
                deltaX = 0.0001
            // Calculate a (a = deltaY/deltaX) and b (b = y2 - ax2)
            a = a * abs(line1.p2.y-line1.p1.y) / deltaX
            b = line1.p2.y - a*line1.p2.x

            //------------------------------------------------------
            // Equation for line2 -> y = cx + d
            //------------------------------------------------------
            var c: Double
            var d: Double
            // Is the curb ascending?
            if((line2.p2.y-line2.p1.y)*(line2.p2.x-line2.p1.x) > 0){
                c = 1.0     // ascending
            }else{
                c = -1.0    // descending
            }
            // deltaX must not be 0
            deltaX = abs(line2.p2.x-line2.p1.x)
            if(deltaX == 0.0)
                deltaX = 0.0001
            // Calculate c (c = deltaY/deltaX) and d (d = y2 - ax2)
            c = c * abs(line2.p2.y-line2.p1.y) / deltaX
            d = line2.p2.y - c*line2.p2.x

            //------------------------------------------------------
            // Find the intersection -> xi = (d-b)/(a-c)
            //                          yi = axi + b
            //------------------------------------------------------
            // If a-c = 0, the lines are parallel. Return null.
            var ac = a-c
            if(ac == 0.0){
                return null
            }else{
                val xi = (d-b)/ac
                val yi = a*xi + b
                return Point(xi,yi)
            }
        }
    }
}