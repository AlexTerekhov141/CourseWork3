package com.packt.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipFile


class ScannedProjectActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var projectPath: String
    private lateinit var linearLayout: LinearLayout
    private lateinit var projectButton: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanned_project)

        projectPath = intent.getStringExtra("projectPath") ?: run {
            Toast.makeText(this, "Project path not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        linearLayout = findViewById(R.id.project_container)

        createProjectButton()
        checkModelStatus()
    }

    private fun createProjectButton() {
        projectButton = Button(this).apply {
            text = projectPath.substringAfterLast('/')
            isEnabled = false
            setOnClickListener {
                downloadZipFile("http://194.226.169.23:5000/download")
            }
        }
        linearLayout.addView(projectButton)
    }

    private fun checkModelStatus() {
        val statusRequest = Request.Builder()
            .url("http://194.226.169.23:5000/status")
            .get()
            .build()

        client.newCall(statusRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CheckStatus", "Error checking status: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@ScannedProjectActivity, "Failed to check status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("CheckStatus", "Response: ${response.code}, body: $responseBody")
                if (response.isSuccessful) {
                    try {
                        val jsonObject = JSONObject(responseBody ?: "")
                        val status = jsonObject.getString("status")
                        val downloadUrl = jsonObject.getString("download_url")

                        runOnUiThread {
                            if (status == "ready") {
                                projectButton.isEnabled = true
                                projectButton.text = "Download ${projectPath.substringAfterLast('/')}"
                                projectButton.setOnClickListener {
                                    downloadZipFile(downloadUrl)
                                }
                            } else {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    checkModelStatus()
                                }, 5000)
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("CheckStatus", "Failed to parse JSON", e)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ScannedProjectActivity, "Failed to check status", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun downloadZipFile(url: String) {
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DownloadZip", "Error downloading file: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@ScannedProjectActivity, "Download failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("DownloadZip", "Server returned error: ${response.code}")
                    return
                }

                val zipFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "3d_model.zip")
                response.body?.byteStream()?.use { inputStream ->
                    FileOutputStream(zipFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                Log.d("DownloadZip", "File downloaded: ${zipFile.absolutePath}")
                runOnUiThread {
                    Toast.makeText(this@ScannedProjectActivity, "File downloaded successfully", Toast.LENGTH_SHORT).show()
                    extractZipFile(zipFile)
                }
            }
        })
    }

    private fun extractZipFile(zipFile: File) {
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "3d_model")
        if (!outputDir.exists()) outputDir.mkdirs()

        try {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val outputFile = File(outputDir, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outputFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            runOnUiThread {
                Toast.makeText(this, "Extraction completed", Toast.LENGTH_SHORT).show()
                openExtractedFolder(outputDir)
            }
        } catch (e: IOException) {
            Log.e("ExtractZip", "Error extracting zip file", e)
        }
    }

    private fun openExtractedFolder(folder: File) {
        val uri = FileProvider.getUriForFile(this, "com.packt.myapplication.fileprovider", folder)
        val intent = Intent(this, ModelViewerActivity::class.java).apply {
            putExtra("modelPath", folder.absolutePath)
        }
        startActivity(intent)
    }

}
