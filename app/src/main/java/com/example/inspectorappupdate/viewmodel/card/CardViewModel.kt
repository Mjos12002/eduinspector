package com.example.inspectorappupdate.viewmodel.card

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inspectorappupdate.model.card.CardModel
import com.example.inspectorappupdate.model.card.CardModelData
import com.example.inspectorappupdate.repository.card.CardRepository

// View model for managing card information
class CardViewModel: ViewModel() {

    // Initialize the mutable and live data of card response
    private var _cardMutableLiveData = MutableLiveData<CardModel>()
    val cardLiveData : LiveData<CardModel> = _cardMutableLiveData

    // The screen showing now; a card read belongs to the screen it was made on.
    private var screen: Int = 0

    /** A new screen opened: the last screen's card is forgotten so it never shows up here. */
    fun onScreenChanged(destination: Int) {
        screen = destination
        _cardMutableLiveData.value = empty()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCardDetails(token: String, cardNumber: String) {
        val forScreen = screen
        val resp = CardRepository().getCardDetails(cardNumber, token)
        // Dropped when the user left the screen while the card was being looked up.
        if (forScreen == screen) _cardMutableLiveData.postValue(resp)
    }

    suspend fun clearCardDetails() {
        _cardMutableLiveData.postValue(empty())
    }

    /** True for the "no card" value set when a screen opens or a screen clears its card; never a real reply. */
    fun isNoCard(card: CardModel?) = card == null || card.status == 0

    private fun empty() = CardModel(0, true, "", CardModelData("", "", "", "", "", "", "", "", 0.toDouble(), 0.toDouble(), "", 0.toDouble(), 0.toDouble(), 0))

}