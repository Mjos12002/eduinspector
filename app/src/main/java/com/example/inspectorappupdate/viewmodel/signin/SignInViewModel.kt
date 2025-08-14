package com.example.inspectorappupdate.viewmodel.signin

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inspectorappupdate.apiresponse.user.LoginResponse
import com.example.inspectorappupdate.model.auth.CredentialsTextLengthCheck
import com.example.inspectorappupdate.model.auth.LoginModel
import com.example.inspectorappupdate.repository.signin.LoginRepository
import com.example.inspectorappupdate.utils.auth.UserAuthUtil

class SignInViewModel: ViewModel() {

    // Private mutable live data of the credentials response
    private var _credentialStatusMutableLiveData = MutableLiveData<CredentialsTextLengthCheck>()
    // Live data of the credentials response
    var credentialStatusLiveData: LiveData<CredentialsTextLengthCheck> = _credentialStatusMutableLiveData

    // Private mutable live data for the login status message
    private var _loginActionStatusMutableLiveData = MutableLiveData<String>()
    var loginStatusLiveData: LiveData<String> = _loginActionStatusMutableLiveData

    // Private mutable live data for login response
    private var _loginResponseMutableLiveData = MutableLiveData<LoginResponse>()
    var loginResponseLiveData: LiveData<LoginResponse> = _loginResponseMutableLiveData

    // Update the login response
    fun updateLoginResponse(loginResponse: LoginResponse) {
        _loginResponseMutableLiveData.postValue(loginResponse)
    }

    // Update login action status
    fun updateLoginStatusMessage(msg: String){
        _loginActionStatusMutableLiveData.postValue(msg)
    }
    // End update the login action status

    // Update the credentials information
    fun updateCredentialStatus(credentialsStatus: CredentialsTextLengthCheck) {
        _credentialStatusMutableLiveData.postValue(credentialsStatus)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun userLogin(username: String, password: String){
        val credentialStatus = UserAuthUtil(username, password)
                .userLoginProcess()
        if (!credentialStatus.error) {
            // Check the user response
            val response = LoginRepository().processLogin(LoginModel(username, password))
            updateLoginResponse(response)
        }else {
            updateCredentialStatus(credentialStatus)
        }

    }

}