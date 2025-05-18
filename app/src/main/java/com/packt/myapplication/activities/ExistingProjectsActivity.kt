package com.packt.myapplication.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.packt.myapplication.adapters.ProjectsAdapter
import com.packt.myapplication.R
import java.io.File

class ExistingProjectsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var projectsAdapter: ProjectsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_existing_projects)
        recyclerView = findViewById(R.id.recyclerViewProjects)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val projects = getExistingProjects()
        projectsAdapter = ProjectsAdapter(projects) { project ->
            val intent = Intent(this, ScannedProjectActivity::class.java)
            intent.putExtra("projectName", project.name)
            intent.putExtra("projectPath", project.absolutePath)
            startActivity(intent)
        }
        recyclerView.adapter = projectsAdapter

    }

    private fun getExistingProjects(): List<File> {
        val galleryPath = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return galleryPath?.listFiles()?.filter { it.isDirectory }?.toList() ?: emptyList()
    }
}
