package com.example.inspectorappupdate.apirequest.issue

import com.example.inspectorappupdate.model.issue.IssueModel
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// API request for issues
interface IIssue {

    @FormUrlEncoded
    @POST("api/ticket-issue")
    suspend fun addTicketIssue(@Field("Authorization") bearer: String, @Field("description") description: String, @Field("title") title: String, @Field("priority") priority: Int, @Field("institute_id") institute_id: Int): Response<IssueModel>

}