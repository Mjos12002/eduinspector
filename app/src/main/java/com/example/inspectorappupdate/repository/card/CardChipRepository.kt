package com.example.inspectorappupdate.repository.card

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.card.ICardChip
import com.example.inspectorappupdate.model.card.ChipCardRequest
import com.example.inspectorappupdate.model.card.ChipPassword
import com.example.inspectorappupdate.model.card.ChipSetupRequest
import com.example.inspectorappupdate.model.card.ChipTapRequest
import com.example.inspectorappupdate.model.card.ChipTapState
import com.example.inspectorappupdate.utils.CardChip
import com.example.inspectorappupdate.utils.RetrofitInstance

/** Chip set-up calls. They never fail a tap: any error gives null and the tap goes on as before. */
class CardChipRepository {

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        private val api: ICardChip = RetrofitInstance.getInstance().create(ICardChip::class.java)
    }

    /** The chip's password; null for a chip that isn't a known card, or when set-up isn't available. */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun password(token: String, uid: String): ChipPassword? = try {
        api.password("Bearer $token", ChipCardRequest(uid)).takeIf { it.isSuccessful }?.body()?.data
    } catch (e: Exception) {
        null
    }

    /** Reports a set-up outcome; DONE and ALREADY are "done", a password not ours is "foreign_password". */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun reportSetup(token: String, uid: String, result: CardChip.SetupResult, signature: String?) {
        val outcome = when (result) {
            CardChip.SetupResult.DONE, CardChip.SetupResult.ALREADY -> "done"
            CardChip.SetupResult.FOREIGN_PASSWORD -> "foreign_password"
            else -> return
        }
        try {
            api.setup("Bearer $token", ChipSetupRequest(uid, outcome, signature))
        } catch (_: Exception) {
        }
    }

    /** Sends what a tap read, from screens that only look the card up; the answer says whether it is set up. */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun tap(token: String, reading: CardChip.Reading, context: String): ChipTapState? {
        if (reading.signature == null && reading.counter == null) return null
        return try {
            api.tap("Bearer $token", ChipTapRequest(reading.uid, reading.signature, reading.counter, context))
                .takeIf { it.isSuccessful }?.body()?.data
        } catch (e: Exception) {
            null
        }
    }
}
