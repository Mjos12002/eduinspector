package com.example.inspectorappupdate.utils

import android.content.Context
import androidx.room.Room
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.ui.fragment.signin.SignInFragment.Companion.appDatabase

class DbUtility() {

    fun dbBuilder(appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            appContext.getString(R.string.roomdbname)
        ).fallbackToDestructiveMigration()
            .build()
    }

    suspend fun getUserToken(): String{
        return appDatabase.loggedInUserDao().getAllData().last().token
    }
}