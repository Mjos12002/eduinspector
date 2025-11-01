package com.example.inspectorappupdate.repository.card

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.card.ICard
import com.example.inspectorappupdate.model.card.CardModel
import com.example.inspectorappupdate.model.card.CardModelData
import com.example.inspectorappupdate.model.card.ChangePinModel
import com.example.inspectorappupdate.model.card.DisablePinModel
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson

// Repository for managing API calls to the card end points
class CardRepository {

    companion object SetCreateCardAPIInterface{
        @RequiresApi(Build.VERSION_CODES.O)
        private val cardInterface: ICard = RetrofitInstance.getInstance().create(ICard::class.java)
    }

    // API request to get the card details
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCardDetails(cardNumber: String, bearer: String): CardModel {
        try{
            val res = cardInterface.getCardByNumber("Bearer $bearer", cardNumber)
            if(res.code() == 200) {
                return res.body()!!
            }else{
                val errorResponse = res.errorBody()?.string()
                val errorResponseObj = Gson().fromJson(errorResponse, CardModel::class.java)
                return CardModel(errorResponseObj.status, errorResponseObj.error, errorResponseObj.message,
                    CardModelData("", "", "", "", "", "", "", "", 0.toDouble(), 0.toDouble(), "", 0.toDouble(), 0.toDouble(), 0))
            }
        }catch (e: Exception) {
            return CardModel(500, true, e.message!!,
                CardModelData("", "", "", "", "", "", "", "", 0.toDouble(), 0.toDouble(), "", 0.toDouble(), 0.toDouble(), 0))
        }
    }

    // Disable card
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun disableCard(indicator: String, token: String): DisablePinModel {
        try {
            val res = cardInterface.disableCard("Bearer ${token}", indicator)
            if(res.code() == 200) {
                return res.body()!!
            }else {
                val errorResponse = res.errorBody()?.string()
                var errorResponseObj = Gson().fromJson(errorResponse, DisablePinModel::class.java)
                return DisablePinModel(errorResponseObj.status, errorResponseObj.error, errorResponseObj.message)
            }
        }catch (e: Exception) {
            return DisablePinModel(500, true, e.message!!)
        }
    }

    // Change PIN
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun changePIN(bearer: String, oldPIN: String, newPIN: String, cardNumber: String): ChangePinModel {

        try {
            val res = cardInterface.changePIN("Bearer $bearer", oldPIN, newPIN, cardNumber)
            if(res.code() == 200) {
                return res.body()!!
            }else {
                val errorBody = res.errorBody()?.string()
                val errorBodyObj = Gson().fromJson(errorBody, ChangePinModel::class.java)
                return ChangePinModel(errorBodyObj.status, errorBodyObj.error, errorBodyObj.message)
            }
        }catch (e: Exception){
            return ChangePinModel(500, true, e.message!!)
        }

    }

}