package com.example.clockhandsdetection091

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*

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
            for(i in 0 until contours.size){
                if(Imgproc.contourArea(contours[i]) > minArea) {
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
            return Math.sqrt(Math.pow((pt2.x-pt1.x),2.0) + Math.pow((pt2.y-pt1.y),2.0))
        }


        //------------------------------------------------------------------------------------
        // Find the biggest rectangle in the picture, get its perspective and warp it into
        // a new bitmap with the same height and a width given by the proportion.
        //------------------------------------------------------------------------------------
        fun transformRectPerspective(src: Bitmap, proportion: Double): Bitmap {
            // Convert to gray
            var grayMat = Mat(src.height, src.width, CvType.CV_8UC4)
            Utils.bitmapToMat(src,grayMat)
            Imgproc.cvtColor(grayMat,grayMat, Imgproc.COLOR_BGR2GRAY)

            // imageMat
            var imageMat = Mat(grayMat.height(), grayMat.width(), CvType.CV_8UC4)
            Utils.bitmapToMat(src, imageMat)

            //Apply a threshold, dilate it and find the contours
            var canny = Mat(grayMat.height(),grayMat.width(), CvType.CV_8UC4)
            //Imgproc.threshold(grayMat, threshold,127.0,255.0, Imgproc.THRESH_BINARY_INV)
            Imgproc.Canny(grayMat,canny,10.0,100.0)
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(6.0,6.0))
            Imgproc.morphologyEx(canny,canny,Imgproc.MORPH_DILATE,kernel)

            var contours:List<MatOfPoint> = ArrayList<MatOfPoint>()
            var hierarchy = Mat()
            Imgproc.findContours(canny,contours,hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            //Get the biggest contour
            var area = 0.0
            var index = 0
            for(i in 0 until contours.size){
                var tempArea = Imgproc.contourArea(contours.get(i))
                if(tempArea > area){
                    area = tempArea
                    index = i
                }
            }
            // Draw the biggest contour
            //Imgproc.drawContours(out,contours,index,Scalar(255.0,0.0,0.0),5)

            // Convert contours(index) from MatOfPoint to MatOfPoint2f and approximate the
            // contour as rectangle
            val contour2f = MatOfPoint2f(*contours.get(index).toArray())
            val approxRect = MatOfPoint2f()
            val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
            Imgproc.approxPolyDP(contour2f, approxRect, approxDistance, true)
            // Convert back to MatOfPoint
            //val points = MatOfPoint(*approxRect.toArray())
            // Imgproc.circle(out,Point(points.get(0,0)[0],points.get(0,0)[1]),10,Scalar(0.0,0.0,255.0),10)

            // Get the 4 corners points and put them in a Mat var, they need to be ordered correctly.
            // To ordered them, I check the two point in the upper part of the picture and set them
            // as topLeft and topRight, same for the lower part.
            var corners = Mat(4,1,CvType.CV_32FC2)
            // Upper part
            var topLeft = Point(imageMat.width().toDouble(),imageMat.height().toDouble())
            var topRight = Point(0.0,imageMat.height().toDouble())
            for(i in 0 until approxRect.rows()){
                var pt = Point(approxRect.get(i,0))
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
                var pt = Point(approxRect.get(i,0))
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
            var newCorners = Mat(4,1,CvType.CV_32FC2)
            val matriceSize = Size(imageMat.height().toDouble()*proportion,
                imageMat.height().toDouble())
            newCorners.put(0,0,0.0,0.0)
            newCorners.put(1,0,matriceSize.width,0.0)
            newCorners.put(2,0,0.0,matriceSize.height)
            newCorners.put(3,0,matriceSize.width,matriceSize.height)

            // Get the perspective transformation and warp it in matriceMat
            var matriceMat = Mat(matriceSize,CvType.CV_8UC4)
            var perspectiveTransform = Imgproc.getPerspectiveTransform(corners,newCorners)
            Imgproc.warpPerspective(imageMat,matriceMat,perspectiveTransform,matriceSize)

            // Create the destination Bitmap and configure it with the matriceMat size
            var dst = Bitmap.createBitmap(matriceMat.width(),matriceMat.height(), Bitmap.Config.RGB_565)
            Utils.matToBitmap(matriceMat, dst)
            return dst
        }
    }
}