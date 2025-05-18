package com.packt.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.packt.myapplication.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class ScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var uploadButton: Button
    private var imageCapture: ImageCapture? = null
    private lateinit var photoCounter: TextView
    private var photoCount = 0
    private val maxPhotos = 30

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        uploadButton = findViewById(R.id.uploadButton)
        photoCounter = findViewById(R.id.photoCounter)

        captureButton.setOnClickListener {
            capturePhoto()
        }

        uploadButton.setOnClickListener {
            uploadPhotos()
        }

        startCamera()

        showNextCapturePrompt()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showNextCapturePrompt() {
        MaterialTapTargetPrompt.Builder(this)
            .setTarget(captureButton)
            .setPrimaryText("Фотографируйте с разных сторон слева/справа/снизу/сверху")
            .setSecondaryText("Нажмите, чтобы сделать следующий снимок")
            .show()
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        val projectPath = intent.getStringExtra("projectPath")
        if (projectPath.isNullOrEmpty()) {
            Toast.makeText(this, "Проект не выбран", Toast.LENGTH_SHORT).show()
            return
        }

        val imagesFolder = File(projectPath, "images")
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs()
        }

        val photoFile = File(imagesFolder, "Screenshot_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@ScannerActivity,
                        "Ошибка сохранения фото: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    photoCount++
                    photoCounter.text = "$photoCount/$maxPhotos"

                    if (photoCount >= maxPhotos) {
                        showUploadPrompt()
                    } else {
                    }
                }
            }
        )
    }

    private fun showUploadPrompt() {
        MaterialTapTargetPrompt.Builder(this)
            .setTarget(uploadButton)
            .setPrimaryText("Загрузите фотографии")
            .setSecondaryText("Нажмите здесь, чтобы отправить сделанные фото")
            .show()
    }

    private fun uploadPhotos() {
        val projectPath = intent.getStringExtra("projectPath")
        val projectName = intent.getStringExtra("projectName")
        if (projectPath.isNullOrEmpty() || projectName.isNullOrEmpty()) {
            Toast.makeText(this, "Проект не выбран", Toast.LENGTH_SHORT).show()
            return
        }

        val imagesFolder = File(projectPath, "images")
        if (!imagesFolder.exists() || !imagesFolder.isDirectory) {
            Toast.makeText(this, "Папка изображений не найдена", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFiles = imagesFolder.listFiles { file -> file.name.endsWith(".jpg") }
        if (photoFiles.isNullOrEmpty()) {
            Toast.makeText(this, "Нет фото для загрузки", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()

        photoFiles.forEach { photo ->
            Log.d("UploadPhotos", "Загрузка файла: ${photo.absolutePath}")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    photo.name,
                    photo.asRequestBody("image/jpg".toMediaTypeOrNull())
                )
                .build()

            val uploadUrl = "http://188.245.194.36:5002/upload?projectName=$projectName"
            val uploadRequest = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            client.newCall(uploadRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("UploadPhotos", "Ошибка загрузки файла: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@ScannerActivity,
                            "Ошибка загрузки для ${photo.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d("UploadPhotos", "Ответ: ${response.code}, тело: $responseBody")
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ScannerActivity,
                                "Загрузка успешна для ${photo.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        if (photo == photoFiles.last()) {
                            create3DModel(client, projectPath)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@ScannerActivity,
                                "Ошибка загрузки для ${photo.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
        }
    }
    private fun create3DModel(client: OkHttpClient, projectPath: String) {
        val projectName = intent.getStringExtra("projectName")
        if (projectName.isNullOrEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Проект не выбран", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val createObjUrl = "http://188.245.194.36:5002/createobj?projectName=$projectName"
        val createObjRequest = Request.Builder()
            .url(createObjUrl)
            .get()
            .build()

        client.newCall(createObjRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CreateObj", "Идет создание 3D модели: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@ScannerActivity,
                        "Идет создание 3D модели(время ожидания до 20 минут)",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@ScannerActivity, ScannedProjectActivity::class.java)
                    intent.putExtra("projectPath", projectPath)
                    startActivity(intent)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("CreateObj", "Ответ: ${response.code}, тело: $responseBody")
                if (response.isSuccessful) {
                    runOnUiThread {
                        val intent = Intent(this@ScannerActivity, ScannedProjectActivity::class.java)
                        intent.putExtra("projectPath", projectPath)
                        startActivity(intent)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@ScannerActivity,
                            "Идет создание 3D модели(время ожидания до 20 минут)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}
