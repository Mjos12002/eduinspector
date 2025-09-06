package com.example.inspectorappupdate.repository.issue

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.issue.IIssue
import com.example.inspectorappupdate.model.issue.IssueModel
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson

// Issue repository
class IssueRepository {

    companion object SetIssueAPIInterface {
        @RequiresApi(Build.VERSION_CODES.O)
        private var issueAPIInterface: IIssue = RetrofitInstance.getInstance().create(IIssue::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addIssue(bearer: String, title: String, description: String, priority: Int, institute_id: Int): IssueModel {
        try {
            val res = issueAPIInterface.addTicketIssue("Bearer $bearer", description, title, priority, institute_id)
            if(res.code() == 200 || res.code() == 200) {
                return res.body()!!
            }else {
                val errorBody = res.errorBody()?.string()
                val errorBodyObj = Gson().fromJson(errorBody, IssueModel::class.java)
                return IssueModel(errorBodyObj.status, errorBodyObj.error, errorBodyObj.message)
            }
        }catch (e: Exception) {
            return IssueModel(500, true, e.message!!)
        }
    }
}