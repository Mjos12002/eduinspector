package com.example.inspectorappupdate.viewmodel.card

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inspectorappupdate.model.card.CardModel
import com.example.inspectorappupdate.repository.card.CardRepository

// View model for managing card information
class CardViewModel: ViewModel() {

    // Initialize the mutable and live data of card response
    private var _cardMutableLiveData = MutableLiveData<CardModel>()
    val cardLiveData : LiveData<CardModel> = _cardMutableLiveData

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCardDetails(token: String, cardNumber: String) {
        val resp = CardRepository().getCardDetails(cardNumber, token)
        _cardMutableLiveData.postValue(resp)
    }



}