package com.packt.myapplication.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.packt.myapplication.R
import org.rajawali3d.view.SurfaceView
import java.io.File

class ModelViewerActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_viewer)

        surfaceView = findViewById(R.id.rajawaliSurface)

        val modelPath = intent.getStringExtra("modelPath") ?: return
        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
            Toast.makeText(this, "Model file does not exist", Toast.LENGTH_SHORT).show()

        }
    }
}