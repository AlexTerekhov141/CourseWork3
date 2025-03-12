package com.packt.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

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
        val projectName = projectPath.substringAfterLast('/')
        projectButton = Button(this).apply {
            text = projectName
            isEnabled = false
            setOnClickListener {
                val intent = Intent(this@ScannedProjectActivity, MainActivity2::class.java)
                intent.putExtra("projectName",projectName)
                startActivity(intent)
            }
        }
        linearLayout.addView(projectButton)
    }

    private fun checkModelStatus() {
        val projectName = projectPath.substringAfterLast('/')
        val statusUrl = "http://188.245.194.36:5002/status?projectName=$projectName"

        val statusRequest = Request.Builder()
            .url(statusUrl)
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
                        runOnUiThread {
                            if (status == "ready") {
                                projectButton.isEnabled = true
                                projectButton.text = "Download $projectName"
                                projectButton.setOnClickListener {
                                    val intent = Intent(this@ScannedProjectActivity, MainActivity2::class.java)
                                    intent.putExtra("projectName", projectName)
                                    startActivity(intent)
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
}
