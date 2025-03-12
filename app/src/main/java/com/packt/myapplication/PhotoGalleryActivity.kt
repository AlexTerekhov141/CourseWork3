package com.packt.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class PhotoGalleryActivity : AppCompatActivity() {

    private lateinit var adapter: PhotoAdapter
    private lateinit var photos: MutableList<File>
    private lateinit var projectNames: List<String>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_gallery)
        val buttonClicked = intent.getStringExtra("button_clicked")
        val recyclerView: RecyclerView = findViewById(R.id.photoRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val btnGallery: ImageButton = findViewById(R.id.btn_gallery)
        val btnHome: ImageButton = findViewById(R.id.btn_home)
        val fabAdd: FloatingActionButton = findViewById(R.id.fab_add)

        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val lastProject = sharedPreferences.getString("last_project", null)
        when (buttonClicked) {
            "last" -> {
                if (lastProject != null) {
                    Toast.makeText(this, lastProject, Toast.LENGTH_SHORT).show()
                    loadPhotos(lastProject)
                } else {
                    Toast.makeText(this, getString(R.string.NoProjectsAvailablToast), Toast.LENGTH_SHORT).show()
                }
            }
            "gallery" -> {
                projectNames = getProjectNames()
                if (projectNames.isNotEmpty()) {
                    showProjectSelectionDialog()
                } else {
                    Toast.makeText(this, getString(R.string.NoProjectsAvailablToast), Toast.LENGTH_SHORT).show()
                }
            }
        }
        /*projectNames = getProjectNames()
        if (projectNames.isNotEmpty()) {
            showProjectSelectionDialog()
        } else {
            Toast.makeText(this, getString(R.string.NoProjectsAvailablToast), Toast.LENGTH_SHORT).show()
        }*/
        btnGallery.setOnClickListener {
            if (this::class.java != PhotoGalleryActivity::class.java) {
            val intent = Intent(this, PhotoGalleryActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
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
        photos = projectFolder.listFiles()?.filter { it.extension == "png" || it.extension == "jpg" || it.extension == "exif" }?.toMutableList() ?: mutableListOf()
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
