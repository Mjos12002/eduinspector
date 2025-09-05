package com.example.inspectorappupdate.repository.payment

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.dto.payment.PaymentDTO
import com.example.inspectorappupdate.apirequest.payment.IPayment
import com.example.inspectorappupdate.model.payment.PaymentTransactionDataModel
import com.example.inspectorappupdate.model.payment.PaymentTransactionModel
import com.example.inspectorappupdate.model.payment.PaymentTypeModel
import com.example.inspectorappupdate.model.payment.PaymentTypeModelData
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson

// Setup the request to the API for payment request
class PaymentRepository {

    companion object SetPaymentAPIInterface {
        @RequiresApi(Build.VERSION_CODES.O)
        private var paymentAPIInterface: IPayment = RetrofitInstance.getInstance().create(IPayment::class.java)
    }

    // Method to get the payment type from API
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getPaymentType(token: String): PaymentTypeModel {
        try {
            val res = paymentAPIInterface.getPaymentType("Bearer $token")
            if (res.code() == 200 || res.code() == 201) {
                return res.body()!!
            }else {
                // In case there is an error
                val errorResponse = res.errorBody()?.string()
                val errorResponseObj = Gson().fromJson(errorResponse, PaymentTypeModel::class.java)
                return PaymentTypeModel(errorResponseObj.status, errorResponseObj.error, errorResponseObj.message,
                    listOf(PaymentTypeModelData(0, "")))
            }
        }catch (e: Exception) {
            return PaymentTypeModel(500, true, e.message!!,
                listOf(PaymentTypeModelData(0, "")))
        }
    }

    // API request to make payment
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun makePayment(token: String, payment: PaymentDTO): PaymentTransactionModel {
        try{
            val amount = payment.amount
            val topup_type_id = payment.topup_type_id
            val payment_type_id = payment.payment_type_id
            val phone = payment.phone
            val student_id = payment.student_id
            val parent_id = payment.parent_id
            val card_number = payment.card_number

            val res = paymentAPIInterface.makePayment("Bearer $token", amount, topup_type_id, payment_type_id, phone, student_id, parent_id, card_number)
            if(res.code() == 200) {
                return res.body()!!
            }else {
                val errorBody = res.errorBody()?.string()
                val errorBodyObj = Gson().fromJson(errorBody, PaymentTransactionModel::class.java)
                return PaymentTransactionModel(errorBodyObj.status, errorBodyObj.error, errorBodyObj.message,
                    PaymentTransactionDataModel(0, 0, ""))
            }
        }catch (e: Exception) {
            return PaymentTransactionModel(500, true, e.message!!,
                PaymentTransactionDataModel(0, 0, ""))
        }
    }

}