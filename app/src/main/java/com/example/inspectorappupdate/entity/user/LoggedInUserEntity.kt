package com.example.inspectorappupdate.entity.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("loggedinuser")
data class LoggedInUserEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val status: String,
    val error: String,
    val message: String,
    @ColumnInfo(name = "first_name")
    val firstName: String,
    @ColumnInfo(name = "last_name")
    val lastName: String,
    @ColumnInfo(name = "contact_number")
    val contactNumber: String,
    @ColumnInfo(name = "reg_number")
    val regNumber: String,
    val email: String,
    val token: String,
    @ColumnInfo(name = "user_id")
    val userID: String,
    val role: String,
    val date: String

)
