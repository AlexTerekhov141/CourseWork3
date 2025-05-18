package com.packt.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.packt.myapplication.R
import java.io.File

class ProjectsAdapter(
    private val projects: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder>() {

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectNameTextView: TextView = itemView.findViewById(R.id.textViewProjectName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        holder.projectNameTextView.text = project.name
        holder.itemView.setOnClickListener { onItemClick(project) }
    }

    override fun getItemCount(): Int = projects.size
}
