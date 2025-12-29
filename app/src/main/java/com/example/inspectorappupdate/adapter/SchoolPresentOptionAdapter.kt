package com.example.inspectorappupdate.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.model.attendance.AttendanceOptionModel

//SchoolPresentOptionAdapter is used to adapter the list of the options of attendance
class SchoolPresentOptionAdapter(private val context: Context, private val items: List<AttendanceOptionModel>) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    // Create and return the view for each item in the spinner
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.school_attendance_option_item, parent, false)

        val item = getItem(position) as AttendanceOptionModel

        // Bind data to views
        val tvItemName = view.findViewById<TextView>(R.id.tv_attendance_item_name)
        tvItemName.text = item.name

        return view
    }

}