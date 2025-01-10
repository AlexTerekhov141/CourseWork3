package com.packt.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ProjectActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        val projectNameInput = findViewById<EditText>(R.id.projectNameInput)
        val createButton = findViewById<Button>(R.id.createProjectButton)
        val selectExistingProject = findViewById<Button>(R.id.selectExistingProjectButton)


        createButton.setOnClickListener {
            val projectName = projectNameInput.text.toString().trim()
            if (projectName.isNotEmpty()) {
                val projectPath = createProjectFolder(projectName)
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("projectName", projectName)
                intent.putExtra("projectPath", projectPath)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Введите имя проекта!", Toast.LENGTH_SHORT).show()
            }
        }


        selectExistingProject.setOnClickListener {
            val projects = getExistingProjects()
            if (projects.isNotEmpty()) {
                showProjectSelectionDialog(projects)
            } else {
                Toast.makeText(this, "Нет доступных проектов.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createProjectFolder(projectName: String): String {
        val galleryPath = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val projectFolder = File(galleryPath, projectName)
        if (!projectFolder.exists()) {
            projectFolder.mkdirs()
        }
        return projectFolder.absolutePath
    }

    private fun getExistingProjects(): List<File> {
        val galleryPath = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val directories = galleryPath?.listFiles()?.filter { it.isDirectory } ?: emptyList()
        return directories
    }

    private fun showProjectSelectionDialog(projects: List<File>) {
        val projectNames = projects.map { it.name }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите проект")

        builder.setItems(projectNames.toTypedArray()) { _, which ->
            val selectedProject = projects[which]
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("projectName", selectedProject.name)
            intent.putExtra("projectPath", selectedProject.absolutePath)
            startActivity(intent)
        }

        builder.setNegativeButton("Отмена", null)
        builder.show()
    }
}
