package com.example.inspectorappupdate.model.wallet

data class WalletModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: List<WalletModelData>
)