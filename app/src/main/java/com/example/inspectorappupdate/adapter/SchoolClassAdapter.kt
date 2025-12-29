package com.example.inspectorappupdate.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.model.schoolclass.SchoolClassModel

//SchoolClassAdapter is used to adapter the information displayed in the spinner for school class
class SchoolClassAdapter(private val context: Context, private val items: List<SchoolClassModel>) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    // Create and return the view for each item in the spinner
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.school_class_item, parent, false)

        val item = getItem(position) as SchoolClassModel

        // Bind data to views
        val tvItemName = view.findViewById<TextView>(R.id.tv_class_item_name)
        tvItemName.text = item.name

        return view
    }

}