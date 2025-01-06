package com.packt.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PhotoGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_gallery)

        val recyclerView: RecyclerView = findViewById(R.id.photoRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)


        val photos = getSavedPhotos()
        if (photos.isNotEmpty()) {
            val adapter = PhotoAdapter(photos) { photo ->
                openPhoto(photo)
            }
            recyclerView.adapter = adapter
        } else {
            Toast.makeText(this, "Нет сохранённых фото.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSavedPhotos(): List<File> {
        val path = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val directory = File(path?.absolutePath ?: "")
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        return directory.listFiles()?.filter { it.extension == "png" } ?: emptyList()
    }

    private fun openPhoto(photo: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(photo), "image/*")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
    }
}
