package com.example.inspectorappupdate.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.model.promptattendance.PromptCategoryModel
import com.example.inspectorappupdate.model.schoolclass.SchoolClassModel

//PromptCategoryAdapter is used to display the prompt category data
class PromptCategoryAdapter(private val context: Context, private val items: List<PromptCategoryModel>): BaseAdapter() {
    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any? {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.prompt_category_item, parent, false)

        val item = getItem(position) as PromptCategoryModel

        // Bind data to views
        val tvItemName = view.findViewById<TextView>(R.id.tv_prompt_attendance_item_name)
        tvItemName.text = item.name

        return view
    }
}