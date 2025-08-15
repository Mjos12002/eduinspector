package com.example.inspectorappupdate.entity.academic_term

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity to model the academic term data
@Entity(tableName = "academicterm")
data class AcademicTermEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "term_id")
    val termID: Int,
    @ColumnInfo("year_id")
    var yearID: Int,
    @ColumnInfo("term_name")
    val termName: String,
    @ColumnInfo(name = "start_date")
    val startDate: String,
    @ColumnInfo("end_date")
    val endDate: String

)
