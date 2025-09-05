package com.example.inspectorappupdate.repository.wallet

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.student.IStudent
import com.example.inspectorappupdate.apirequest.wallet.IWallet
import com.example.inspectorappupdate.model.wallet.WalletModel
import com.example.inspectorappupdate.model.wallet.WalletModelData
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson

// Repository to access wallet data
class WalletRepository {

    companion object SetWalletAPIInterface {
        @RequiresApi(Build.VERSION_CODES.O)
        private var walletAPIInterface: IWallet = RetrofitInstance.getInstance().create(IWallet::class.java)
    }

    // Method to access the wallet API
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getWallet(token: String): WalletModel {
        try{
            // Get data fom wallet API
            val res = walletAPIInterface.getWallet("Bearer $token")
            if(res.code() == 200 || res.code() == 201) {
                return res.body()!!
            }else {
                // In case there is an error, response code not 200
                val errorResponse = res.errorBody()?.string()
                var errorResponseObj = Gson().fromJson(errorResponse, WalletModel::class.java)
                return WalletModel(errorResponseObj.status, errorResponseObj.error, errorResponseObj.message,
                    listOf(WalletModelData(0, "")))
            }

        }catch (e: Exception) {
            // If there is an exception return 500 error code
            return WalletModel(500, true, e.message!!, listOf(WalletModelData(0, "")))
        }
    }
}