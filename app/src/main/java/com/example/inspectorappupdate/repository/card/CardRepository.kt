package com.example.inspectorappupdate.repository.card

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.card.ICard
import com.example.inspectorappupdate.model.card.CardResponse
import com.example.inspectorappupdate.model.card.CardResponseData
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson

// Repository for managing API calls to the card end points
class CardRepository {

    companion object SetCreateCardAPIInterface{
        @RequiresApi(Build.VERSION_CODES.O)
        private val createCardInterface: ICard = RetrofitInstance.getInstance().create(ICard::class.java)
    }

    // API request to get the card details
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCardDetails(cardNumber: String, bearer: String): CardResponse {
        try{
            val res = createCardInterface.getCardByNumber("Bearer $bearer", cardNumber)
            if(res.code() == 200) {
                return res.body()!!
            }else{
                val errorResponse = res.errorBody()?.string()
                val errorResponseObj = Gson().fromJson(errorResponse, CardResponse::class.java)
                return CardResponse(errorResponseObj.status, errorResponseObj.error, errorResponseObj.message,
                    CardResponseData("", "", "", "", "", "", "", "", 0, 0, "", 0, 0, 0))
            }
        }catch (e: Exception) {
            return CardResponse(500, true, e.message!!,
                CardResponseData("", "", "", "", "", "", "", "", 0, 0, "", 0, 0, 0))
        }
    }

}