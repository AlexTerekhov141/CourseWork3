package com.packt.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView

class StartActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val startButton = findViewById<Button>(R.id.startButton)
        val opencvButton = findViewById<Button>(R.id.startButtonOpencv)
        val viewPhotosButton = findViewById<Button>(R.id.viewPhotosButton)

        logoImage.alpha = 0f
        logoImage.animate().alpha(1f).setDuration(1500).start()


        startButton.alpha = 0f
        startButton.animate().alpha(1f).setDuration(2000).start()

        opencvButton.alpha = 0f
        opencvButton.animate().alpha(1f).setDuration(2000).start()

        viewPhotosButton.alpha = 0f
        viewPhotosButton.animate().alpha(1f).setDuration(2000).start()

        startButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        opencvButton.setOnClickListener {
            val intent = Intent(this, OpenCVAcitivity::class.java)
            startActivity(intent)
        }


        viewPhotosButton.setOnClickListener {
            val intent = Intent(this, PhotoGalleryActivity::class.java)
            startActivity(intent)
        }

    }
}
