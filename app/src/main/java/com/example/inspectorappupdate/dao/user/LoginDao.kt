package com.example.inspectorappupdate.dao.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.inspectorappupdate.entity.user.LoggedInUserEntity

@Dao
interface LoginDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: LoggedInUserEntity)

    @Query("SELECT * FROM loggedinuser")
    suspend fun getAllData(): List<LoggedInUserEntity>

    @Query("SELECT * FROM loggedinuser ORDER BY id DESC LIMIT 1")
    suspend fun getLastLogin(): LoggedInUserEntity

}