package com.example.inspectorappupdate.utils

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.dao.academic_term.AcademicTermDao
import com.example.inspectorappupdate.dao.user.LoginDao
import com.example.inspectorappupdate.entity.academic_term.AcademicTermEntity
import com.example.inspectorappupdate.entity.user.LoggedInUserEntity

@Database(entities = [LoggedInUserEntity::class, AcademicTermEntity::class], version = 2)
abstract class AppDatabase: RoomDatabase() {

    abstract fun loggedInUserDao(): LoginDao
    abstract fun academicTermDao(): AcademicTermDao

    fun getInstance(context: Context): AppDatabase {
        return databaseBuilder<AppDatabase>(
                context.applicationContext,
                AppDatabase::class.java,
                context.getString(R.string.roomdbname)
            ).fallbackToDestructiveMigration(false)
            .build()
    }

}