package com.packt.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

class OpenCVAcitivity : AppCompatActivity() {

    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var imageView: ImageView

    lateinit var yoloNet: Net
    private val confThreshold = 0.5

    var receivedDistance: Float = 0f

    var sensorWidth = 5.76f
    var sensorHeight = 4.29f
    var focalLength = 4.2f

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.opencv_main)
        getPermission()
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)

        receivedDistance = intent.getFloatExtra("distance", 0f)
        Log.d("OpenCVActivity", "Полученное расстояние: $receivedDistance")

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Не удалось загрузить OpenCV!")
        } else {
            Log.d("OpenCV", "OpenCV загружен успешно!")
            initYOLO()
        }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                processFrameYOLO()
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()!!
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        sensorWidth = sensorSize?.width!!
        sensorHeight = sensorSize?.height!!
        Toast.makeText(this, "f=$focalLength, sw=$sensorWidth, sh=$sensorHeight", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surfaceTexture = textureView.surfaceTexture
                val surface = Surface(surfaceTexture)
                val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(captureRequest.build(), null, handler)
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, handler)
            }
            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, handler)
    }

    fun getPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission()
        }
    }

    private fun initYOLO() {
        val cfgPath = copyAssetFile("yolov3-tiny.cfg")
        val weightsPath = copyAssetFile("yolov3-tiny2.weights")
        yoloNet = Dnn.readNetFromDarknet(cfgPath, weightsPath)
        Log.d("YOLO", "YOLO-сеть загружена")
    }

    private fun copyAssetFile(filename: String): String {
        val file = File(filesDir, filename)
        if (!file.exists()) {
            assets.open(filename).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return file.absolutePath
    }

    private fun getOutputNames(net: Net): List<String> {
        val names = mutableListOf<String>()
        val outLayers = net.unconnectedOutLayers
        val layersNames = net.layerNames
        for (i in 0 until outLayers.total().toInt()) {
            val idx = outLayers.get(i, 0)[0].toInt() - 1
            names.add(layersNames[idx])
        }
        return names
    }

    fun processFrameYOLO() {
        val originalBitmap = textureView.bitmap ?: return
        val mat = Mat()
        Utils.bitmapToMat(originalBitmap, mat)

        if (mat.channels() == 4) {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR)
        }

        val blob = Dnn.blobFromImage(mat, 1.0 / 255.0, Size(416.0, 416.0), Scalar(0.0, 0.0, 0.0), true, false)
        yoloNet.setInput(blob)
        val outNames = getOutputNames(yoloNet)
        val outputs = ArrayList<Mat>()
        yoloNet.forward(outputs, outNames)

        val cols = mat.cols().toDouble()
        val rows = mat.rows().toDouble()

        for (output in outputs) {
            for (i in 0 until output.rows()) {
                val data = FloatArray(output.cols())
                output.get(i, 0, data)
                val confidence = data[4]
                if (confidence > confThreshold) {
                    val classScores = data.sliceArray(5 until data.size)
                    val maxScore = classScores.maxOrNull() ?: 0f
                    if (maxScore > confThreshold) {
                        val centerX = data[0] * cols
                        val centerY = data[1] * rows
                        val width = data[2] * cols
                        val height = data[3] * rows
                        val left = centerX - width / 2
                        val top = centerY - height / 2

                        Imgproc.rectangle(mat, Point(left, top), Point(left + width, top + height), Scalar(0.0, 255.0, 0.0), 2)

                        val text = if (receivedDistance > 0f) {
                            val realWidth = (width / cols) * (receivedDistance * sensorWidth / focalLength) * 100.0
                            val realHeight = (height / rows) * (receivedDistance * sensorHeight / focalLength) * 100.0
                            "W: ${realWidth.toInt()} cm, H: ${realHeight.toInt()} cm"
                        } else {
                            "W: ${width.toInt()}, H: ${height.toInt()}"
                        }
                        Imgproc.putText(mat, text, Point(left, top - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, Scalar(0.0, 255.0, 0.0), 2)
                    }
                }
            }
        }

        val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap)
        runOnUiThread {
            imageView.setImageBitmap(resultBitmap)
        }
    }
}
