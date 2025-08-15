package com.example.inspectorappupdate.dao.academic_term

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.inspectorappupdate.entity.academic_term.AcademicTermEntity

// Data access object to manage the academic term
@Dao
interface AcademicTermDao {

    // Create new record for the academic term
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: AcademicTermEntity)

    // Get the last record of the academic term
    @Query("SELECT * FROM academicterm ORDER BY id DESC LIMIT 1")
    suspend fun getLastAcademicTerm(): AcademicTermEntity

}
