package com.packt.myapplication

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.rajawali3d.Object3D
import org.rajawali3d.loader.LoaderOBJ
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.view.SurfaceView
import org.rajawali3d.renderer.Renderer
import java.io.File
import java.io.FileInputStream
import java.io.IOException

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