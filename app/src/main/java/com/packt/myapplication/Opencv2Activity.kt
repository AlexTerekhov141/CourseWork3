package com.packt.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
import androidx.appcompat.app.AppCompatActivity

class Opencv2Activity : CameraActivity(), CvCameraViewListener2 {

    private var mConfigBuffer: MatOfByte? = null
    private var mModelBuffer: MatOfByte? = null
    private var net: Net? = null
    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    lateinit var cameraManager: CameraManager
    private var receivedDistance: Float = 0f

    var sensorWidth = 5.76f
    var sensorHeight = 4.29f
    var focalLength = 4.2f

    companion object {
        private const val TAG = "Opencv2Activity"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully")
        } else {
            Log.e(TAG, "OpenCV initialization failed!")
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show()
            return
        }
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()!!
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        sensorWidth = sensorSize?.width!!
        sensorHeight = sensorSize?.height!!

        mModelBuffer = loadFileFromAssets("mobilenet_iter_73000.caffemodel")
        mConfigBuffer = loadFileFromAssets("deploy.prototxt")
        if (mModelBuffer == null || mConfigBuffer == null) {
            Log.e(TAG, "Failed to load model from resources")
        } else {
            Log.i(TAG, "Model files loaded successfully")
        }

        net = Dnn.readNet("caffe", mModelBuffer, mConfigBuffer)
        Log.i(TAG, "Network loaded successfully")

        setContentView(R.layout.opencv2_activity)
        mOpenCvCameraView = findViewById<View>(R.id.CameraView) as CameraBridgeViewBase
        mOpenCvCameraView!!.visibility = CameraBridgeViewBase.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)

        receivedDistance = intent.getFloatExtra("distance", 0f)
        Log.d(TAG, "Received distance: $receivedDistance m")
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

    override fun onResume() {
        super.onResume()
        mOpenCvCameraView?.enableView()
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView?.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView?.disableView()
        mModelBuffer?.release()
        mConfigBuffer?.release()
    }

    override fun getCameraViewList(): List<CameraBridgeViewBase> {
        return listOf(mOpenCvCameraView!!)
    }

    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        val IN_WIDTH = 300
        val IN_HEIGHT = 300
        val IN_SCALE_FACTOR = 0.007843
        val MEAN_VAL = 127.5
        val THRESHOLD = 0.2

        val frame = inputFrame.rgba()
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)

        val blob = Dnn.blobFromImage(
            frame, IN_SCALE_FACTOR,
            Size(IN_WIDTH.toDouble(), IN_HEIGHT.toDouble()),
            Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false, false
        )
        net!!.setInput(blob)
        var detections = net!!.forward()

        val cols = frame.cols().toDouble()
        val rows = frame.rows().toDouble()

        detections = detections.reshape(1, detections.total().toInt() / 7)

        for (i in 0 until detections.rows()) {
            val confidence = detections[i, 2][0]
            if (confidence > THRESHOLD) {
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

                val displayText = if (receivedDistance > 0f) {
                    val realWidth = (objectWidth.toDouble() / cols) * (receivedDistance * 100 * sensorWidth / focalLength)
                    val realHeight = (objectHeight.toDouble() / cols) * (receivedDistance * 100 * sensorHeight / focalLength)
                    "W: ${realWidth.toInt()} cm, H: ${realHeight.toInt()} cm"
                } else {
                    "W: $objectWidth, H: $objectHeight"
                }

                Imgproc.putText(
                    frame, displayText, Point(left.toDouble(), top.toDouble()),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, Scalar(255.0, 255.0, 0.0)
                )
            }
        }

        return frame
    }
}
