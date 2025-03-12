package com.packt.myapplication

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class StartActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start2)

        val startButton = findViewById<Button>(R.id.outlined_btn4)
        val opencvButton = findViewById<Button>(R.id.outlined_btn6)
        val viewPhotosButton = findViewById<Button>(R.id.outlined_btn3)
        val scanner3dButton = findViewById<Button>(R.id.outlined_btn7)
        val youtubeButton = findViewById<Button>(R.id.outlined_btn)
        val TelegramButton = findViewById<Button>(R.id.outlined_btn2)
        val btnGallery: ImageButton = findViewById(R.id.btn_gallery)
        val btnHome: ImageButton = findViewById(R.id.btn_home)
        val fabAdd: FloatingActionButton = findViewById(R.id.fab_add)

        youtubeButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/"))
            intent.setPackage("com.google.android.youtube")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/")))
            }
        }
        TelegramButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://telegra.ph/Armeasure-gajd-po-knopkam-03-12"))
            intent.setPackage("org.telegram.messenger")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://telegra.ph/Armeasure-gajd-po-knopkam-03-12")))
            }
        }
        startButton.setOnClickListener {
            val intent = Intent(this,ProjectActivity::class.java)
            startActivity(intent)
        }


        opencvButton.setOnClickListener {
            val intent = Intent(this, ARActivity::class.java)
            startActivity(intent)
        }
        scanner3dButton.setOnClickListener {
            val intent = Intent(this, Project3dActivity::class.java)
            startActivity(intent)
        }

        viewPhotosButton.setOnClickListener {
            val intent = Intent(this, PhotoGalleryActivity::class.java)
            intent.putExtra("button_clicked", "last")
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        btnGallery.setOnClickListener {
            val intent = Intent(this, Project2Activity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        btnHome.setOnClickListener {
            if (this::class.java != StartActivity::class.java) {
                Toast.makeText(this, "Переход на главный экран", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, StartActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }

        fabAdd.setOnClickListener {
            val intent = Intent(this, ProjectActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }


    }
    override fun onResume() {
        super.onResume()
        updateLastProjectInfo()
    }

    private fun updateLastProjectInfo() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val lastProject = sharedPreferences.getString("last_project", "Нет проектов")

        val galleryPath = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val projectFolder = File(galleryPath, lastProject ?: "")
        val photoCount = projectFolder.listFiles()?.count { it.extension in listOf("png", "jpg", "exif") } ?: 0

        findViewById<TextView>(R.id.lastProjectName).text = lastProject
        findViewById<TextView>(R.id.photoCount).text = "Кол-во фото: $photoCount"
    }
}
