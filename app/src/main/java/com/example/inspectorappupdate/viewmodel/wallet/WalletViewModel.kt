package com.example.inspectorappupdate.viewmodel.wallet

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inspectorappupdate.model.wallet.WalletModel
import com.example.inspectorappupdate.repository.wallet.WalletRepository

// View model for wallet
class WalletViewModel: ViewModel() {

    // Initialization of the variables
    private var _walletMutableLiveData = MutableLiveData<WalletModel>()
    val walletLiveData: LiveData<WalletModel> = _walletMutableLiveData

    // Method used to get the wallet data
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getWallet(token: String) {
        try{
            val res = WalletRepository().getWallet(token)
            _walletMutableLiveData.postValue(res)
        }catch (e: Exception) {

        }
    }

}