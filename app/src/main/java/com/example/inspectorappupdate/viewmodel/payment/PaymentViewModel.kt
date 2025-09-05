package com.example.inspectorappupdate.viewmodel.payment

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inspectorappupdate.model.payment.PaymentTypeModel
import com.example.inspectorappupdate.repository.payment.PaymentRepository

// View model for the payment
class PaymentViewModel: ViewModel() {

    // Initialize the key variables
    private var _paymentMutableLiveData = MutableLiveData<PaymentTypeModel>()
    val paymentLiveData: LiveData<PaymentTypeModel> = _paymentMutableLiveData

    // Method used to get the payment types
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getPaymentType(token: String) {
        try {
            val res = PaymentRepository().getPaymentType("Bearer $token")
            _paymentMutableLiveData.postValue(res)
        }catch (e: Exception) { }
    }

}