package com.packt.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.packt.myapplication.R

class Project2Activity: AppCompatActivity() {
    private lateinit var galleryButton: Button
    private lateinit var projectButton : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.project2_activity)
        val btnHome: ImageButton = findViewById(R.id.btn_home)
        val fabAdd: FloatingActionButton = findViewById(R.id.fab_add)
        galleryButton = findViewById(R.id.createProjectButton)
        projectButton = findViewById(R.id.selectExistingProjectButton)
        galleryButton.setOnClickListener {
            val intent = Intent(this, PhotoGalleryActivity::class.java)
            intent.putExtra("button_clicked", "gallery")
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        projectButton.setOnClickListener {
            val intent = Intent(this, ExistingProjectsActivity::class.java)
            startActivity(intent)
        }
        btnHome.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        fabAdd.setOnClickListener {
            val intent = Intent(this, ProjectActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }
}