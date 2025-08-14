package com.example.inspectorappupdate.apiresponse.user

data class LoginResponseData(
    var first_name: String,
    var last_name: String,
    var contact_number: String,
    var reg_number: String,
    var email: String,
    var token: String,
    var user_id: String,
    var role: String
) {}
