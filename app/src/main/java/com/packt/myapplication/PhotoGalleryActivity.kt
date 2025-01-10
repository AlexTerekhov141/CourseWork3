package com.packt.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PhotoGalleryActivity : AppCompatActivity() {

    private lateinit var adapter: PhotoAdapter
    private lateinit var photos: MutableList<File>
    private lateinit var projectNames: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_gallery)

        val recyclerView: RecyclerView = findViewById(R.id.photoRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)


        projectNames = getProjectNames()
        if (projectNames.isNotEmpty()) {
            showProjectSelectionDialog()
        } else {
            Toast.makeText(this, getString(R.string.NoProjectsAvailablToast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getProjectNames(): List<String> {
        val galleryPath = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val directories = galleryPath?.listFiles()?.filter { it.isDirectory } ?: emptyList()
        return directories.map { it.name }
    }

    private fun showProjectSelectionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.ChooseExisting))

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, projectNames)
        builder.setAdapter(adapter) { _, which ->
            val projectName = projectNames[which]
            loadPhotos(projectName)
        }
        builder.setNegativeButton(getString(R.string.CancelButton), null)
        builder.show()
    }

    private fun loadPhotos(projectName: String) {
        val projectFolder = File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), projectName)
        photos = projectFolder.listFiles()?.filter { it.extension == "png" }?.toMutableList() ?: mutableListOf()
        if (photos.isNotEmpty()) {
            adapter = PhotoAdapter(
                photos,
                onClick = { photo -> openPhoto(photo) }
            )
            findViewById<RecyclerView>(R.id.photoRecyclerView).adapter = adapter
        } else {
            Toast.makeText(this,
                getString(R.string.NoPhotosInProjectToast, projectName), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPhoto(photo: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            photo
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.CantOpenPhotoToast), Toast.LENGTH_SHORT).show()
            Log.e("PhotoGallery", getString(R.string.ErrorPhotoOpenning, e.message))
        }
    }

}
