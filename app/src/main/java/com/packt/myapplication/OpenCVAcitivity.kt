package com.packt.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.IOException

class OpenCVAcitivity : CameraActivity(), CvCameraViewListener2 {
    public override fun onResume() {
        super.onResume()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.enableView()
    }
    private fun loadFileFromAssets(fileName: String): MatOfByte? {
        return try {
            val `is` = assets.open(fileName)
            val buffer = ByteArray(`is`.available())
            `is`.read(buffer)
            `is`.close()
            MatOfByte(*buffer)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load file from assets! Exception: $e")
            Toast.makeText(this, "Failed to load file from assets!", Toast.LENGTH_LONG).show()
            null
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully")
        } else {
            Log.e(TAG, "OpenCV initialization failed!")
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)
                .show()
            return
        }

        mModelBuffer = loadFileFromAssets("mobilenet_iter_73000.caffemodel")
        mConfigBuffer = loadFileFromAssets("deploy.prototxt")
        if (mModelBuffer == null || mConfigBuffer == null) {
            Log.e(TAG, "Failed to load model from resources")
        } else Log.i(TAG, "Model files loaded successfully")

        net = Dnn.readNet("caffe", mModelBuffer, mConfigBuffer)
        Log.i(TAG, "Network loaded successfully")

        setContentView(R.layout.opencv_main)



        mOpenCvCameraView = findViewById<View>(R.id.CameraView) as CameraBridgeViewBase
        mOpenCvCameraView!!.visibility = CameraBridgeViewBase.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
    }

    public override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    override fun getCameraViewList(): List<CameraBridgeViewBase> {
        return listOf(mOpenCvCameraView!!)
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()

        mModelBuffer!!.release()
        mConfigBuffer!!.release()
    }


    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        val IN_WIDTH = 300
        val IN_HEIGHT = 300
        val IN_SCALE_FACTOR = 0.007843
        val MEAN_VAL = 127.5
        val THRESHOLD = 0.2



        Log.d(TAG, "handle new frame!")
        val frame = inputFrame.rgba()
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)



        val blob = Dnn.blobFromImage(
            frame, IN_SCALE_FACTOR,
            Size(IN_WIDTH.toDouble(), IN_HEIGHT.toDouble()),
            Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL),  false,  false
        )
        net!!.setInput(blob)
        var detections = net!!.forward()

        val cols = frame.cols()
        val rows = frame.rows()

        detections = detections.reshape(1, detections.total().toInt() / 7)


        for (i in 0 until detections.rows()) {

            val confidence = detections[i, 2][0]
            if (confidence > THRESHOLD) {
                val classId = detections[i, 1][0].toInt()


                val left = (detections[i, 3][0] * cols).toInt()
                val top = (detections[i, 4][0] * rows).toInt()
                val right = (detections[i, 5][0] * cols).toInt()
                val bottom = (detections[i, 6][0] * rows).toInt()


                val objectWidth = right - left
                val objectHeight = bottom - top




                Imgproc.rectangle(
                    frame,
                    Point(left.toDouble(), top.toDouble()),
                    Point(right.toDouble(), bottom.toDouble()),
                    Scalar(0.0, 255.0, 0.0)
                )

                Imgproc.putText(
                    frame, "W = $objectWidth, H = $objectHeight", Point(left.toDouble(), top.toDouble()),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, Scalar(255.0, 255.0, 0.0)
                )
            }
        }

        return frame
    }

    override fun onCameraViewStopped() {}

    private var mConfigBuffer: MatOfByte? = null
    private var mModelBuffer: MatOfByte? = null
    private var net: Net? = null
    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    companion object {
        private const val TAG = "OpenCV-MobileNet"
    }
}
