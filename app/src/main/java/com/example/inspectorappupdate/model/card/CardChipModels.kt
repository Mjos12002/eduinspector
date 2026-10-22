package com.example.inspectorappupdate.model.card

import com.google.gson.annotations.SerializedName

data class ChipEnvelope<T>(
    @SerializedName("status") val status: Int,
    @SerializedName("error") val error: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

data class ChipCardRequest(@SerializedName("card_number") val cardNumber: String)

/** The chip's password and the answer it gives to it (PACK), in hex. */
data class ChipPassword(
    @SerializedName("pwd") val pwd: String,
    @SerializedName("pack") val pack: String
)

data class ChipSetupRequest(
    @SerializedName("card_number") val cardNumber: String,
    @SerializedName("result") val result: String,
    @SerializedName("signature") val signature: String? = null
)

data class ChipTapRequest(
    @SerializedName("card_number") val cardNumber: String,
    @SerializedName("signature") val signature: String?,
    @SerializedName("counter") val counter: Int?,
    @SerializedName("context") val context: String
)

data class ChipTapState(
    @SerializedName("card_id") val cardId: Int,
    @SerializedName("setup") val setup: Boolean
)
