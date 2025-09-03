package com.example.inspectorappupdate.viewmodel.offense

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inspectorappupdate.model.offense.OffenseModel
import com.example.inspectorappupdate.repository.offense.OffenseRepository

// View model for managing the offenses
class OffenseViewModel: ViewModel() {

    // Initialization of the offense mutable live data
    private var _offenseMutableLiveData = MutableLiveData<OffenseModel>()
    var offenseLiveData: LiveData<OffenseModel> = _offenseMutableLiveData

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentOffense(token: String, studentID: Int, termID: Int) {

        val offenseModel = OffenseRepository().getStudentOffense(token, studentID, termID)
        _offenseMutableLiveData.postValue(offenseModel)

    }

}