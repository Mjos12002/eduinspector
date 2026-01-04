package com.example.inspectorappupdate.apirequest.student

import com.example.inspectorappupdate.dto.student.StudentAttendanceDTO
import com.example.inspectorappupdate.dto.student.StudentPermissionTapOutDTO
import com.example.inspectorappupdate.dto.student.StudentPromptAttendanceDTO
import com.example.inspectorappupdate.model.student.SchoolAttendanceResponse
import com.example.inspectorappupdate.model.student.StudentAttendanceResponse
import com.example.inspectorappupdate.model.student.StudentModel
import com.example.inspectorappupdate.model.student.StudentSearchResponse
import com.example.inspectorappupdate.model.studentpermission.StudentPermissionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

//IStudent is an interface used to manage the activities related to student such as search student, get student and attendance management
interface IStudent {

    // searchStudentByRegistrationNumber is used to search the student by registration number
    @GET("api/show/{reg_number}/student")
    suspend fun searchStudentByRegistrationNumber(@Header("Authorization") bearer: String,  @Path("reg_number") reg_number: String): Response<StudentModel>

    //getStudentByRegistrationNumber is used to get the student by registration number
    @GET("api/show/{reg_number}/student")
    suspend fun getStudentByRegistrationNumber(@Header("Authorization") bearer: String,  @Path("reg_number") reg_number: String): Response<StudentSearchResponse>

    //addSchoolAttendance is used to create a new attendance in
    @FormUrlEncoded
    @POST("/api/attendance/check-in")
    suspend fun addSchoolAttendanceIn(@Header("Authorization") bearer: String, @Field("card_number") card_number: String, @Field("device_id") device_id: Int): Response<SchoolAttendanceResponse>

    //addSchoolAttendance is used to create a new attendance out
    @FormUrlEncoded
    @POST("/api/attendance/check-out")
    suspend fun addSchoolAttendanceOut(@Header("Authorization") bearer: String, @Field("card_number") card_number: String, @Field("device_id") device_id: Int): Response<SchoolAttendanceResponse>

    // getStudentPermission is used to get the permissions of a student
    @GET("/api/visit-permissions-student/{student_id}")
    suspend fun getStudentPermission(@Header("Authorization") bearer: String, @Path("student_id") studentID: String): Response<StudentPermissionResponse>

    //CreateStudentAttendance is used to call the api to create attendance record
    @POST("/api/class-attendance-record")
    suspend fun createStudentAttendance(@Header("Authorization") bearer: String, @Body studentAttendance: StudentAttendanceDTO): Response<StudentAttendanceResponse>

    //createStudentPromptAttendance is used to call the api to create the prompt attendance record
    @POST("/api/prompt-attendance-record")
    suspend fun createStudentPromptAttendance(@Header("Authorization") bearer: String, @Body studentPromptAttendance: StudentPromptAttendanceDTO): Response<StudentAttendanceResponse>

    // createStudentPermissionTapOut is used to call the api to create the tap out for the permission
    @POST("/api/visit-permissions/tap-out")
    suspend fun createStudentPermissionTapOut(@Header("Authorization") bearer: String, @Body tapOutDTO: StudentPermissionTapOutDTO): Response<StudentAttendanceResponse>

    // createStudentPermissionTapIn is to used to call the api to create the tap in for the permission
    @POST("/api/visit-permissions/tap-in")
    suspend fun createStudentPermissionTapIn(@Header("Authorization") bearer: String, @Body tapOutDTO: StudentPermissionTapOutDTO): Response<StudentAttendanceResponse>

}