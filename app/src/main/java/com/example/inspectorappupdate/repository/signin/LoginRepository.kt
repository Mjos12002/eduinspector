package com.example.inspectorappupdate.repository.signin

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.user.ILogin
import com.example.inspectorappupdate.apiresponse.user.LoginResponse
import com.example.inspectorappupdate.apiresponse.user.LoginResponseData
import com.example.inspectorappupdate.model.auth.LoginModel
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson

// Login Repository used to connect to the remote data source
class LoginRepository() {

    companion object SetUserAPIInterface {
        @RequiresApi(Build.VERSION_CODES.O)
        private var userAPIInterface: ILogin = RetrofitInstance.getInstance().create(ILogin::class.java)
    }

    // Method used to process user login
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun processLogin(loginModel: LoginModel): LoginResponse {

        try{
            val response = userAPIInterface.login(loginModel.username, loginModel.password)
            if(response.code() == 200) {
                return response.body()!!
            }else{
                val error = response.errorBody()?.string()
                val errorObj = Gson().fromJson(error, LoginResponse::class.java)
                val message = errorObj.message
                val err = errorObj.error
                val status = errorObj.status
                return LoginResponse(status, err, message,
                    LoginResponseData("", "", "", "", "", "", "", "")
                )
            }
        }catch (e: Exception) {
            return LoginResponse("500", e.message!!, e.message!!, LoginResponseData("", "", "", "", "", "", "", ""))
        }
    }

}