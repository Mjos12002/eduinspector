package com.example.inspectorappupdate.ui.fragment.pin

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.databinding.FragmentChangePinBinding
import com.example.inspectorappupdate.model.card.CardModelData
import com.example.inspectorappupdate.repository.card.CardRepository
import com.example.inspectorappupdate.ui.fragment.card.CardFormUi
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import kotlinx.coroutines.launch

/** New PIN for the tapped card: the student types it twice on the keypad; the old PIN isn't asked for. */
class ChangePinFragment : Fragment() {

    private var _binding: FragmentChangePinBinding? = null
    private val binding get() = _binding!!

    private val cardViewModel: CardViewModel by activityViewModels()

    private var card: CardModelData? = null
    private var first = ""
    private var typed = ""
    private var repeat = false
    private var saving = false
    private var saved = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChangePinBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CardFormUi.keypad(binding.glKeypad, ::type, ::delete)
        binding.btnSave.setOnClickListener { save() }

        cardViewModel.cardLiveData.observe(viewLifecycleOwner) { model ->
            if (cardViewModel.isNoCard(model) || model.error || model.data == null) return@observe
            if (model.data.card_number == card?.card_number) return@observe
            card = model.data
            restart(null)
        }
        restart(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun restart(message: String?) {
        first = ""
        typed = ""
        repeat = false
        saved = false
        render()
        CardFormUi.result(binding.tvResult, message, ok = false)
    }

    private fun type(digit: Char) {
        if (saving || saved || typed.length >= PIN_LENGTH) return
        typed += digit
        if (typed.length == PIN_LENGTH) {
            if (!repeat) {
                first = typed
                typed = ""
                repeat = true
            } else if (typed != first) {
                restart(getString(R.string.pin_mismatch))
                return
            }
        }
        render()
    }

    private fun delete() {
        if (saving || saved || typed.isEmpty()) return
        typed = typed.dropLast(1)
        render()
    }

    private fun render() {
        if (_binding == null) return
        val ready = repeat && typed == first
        binding.tvPinTitle.text = getString(if (repeat) R.string.pin_new_again else R.string.pin_new_first)
        binding.tvPinHint.text = getString(if (ready && !saved) R.string.pin_ready else R.string.pin_new_hint)
        CardFormUi.pinDots(binding.llPinDots, PIN_LENGTH, typed.length)
        CardFormUi.result(binding.tvResult, null, ok = true)
        CardFormUi.styleButton(binding.btnSave, card != null && ready && !saving && !saved)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun save() {
        val current = card ?: return
        val pin = first
        saving = true
        binding.btnSave.text = getString(R.string.pin_saving)
        CardFormUi.styleButton(binding.btnSave, false)
        viewLifecycleOwner.lifecycleScope.launch {
            val token = DbUtility().dbBuilder(requireContext()).loggedInUserDao().getLastLogin().token
            // Staff set a new PIN without the old one; the API checks the card is at their school.
            val res = CardRepository().changePIN(token, "", pin, current.card_number)
            saving = false
            if (_binding == null) return@launch
            binding.btnSave.text = getString(R.string.pin_save)
            if (!res.error) {
                saved = true
                render()
                CardFormUi.result(binding.tvResult, getString(R.string.pin_saved), ok = true)
            } else {
                restart(res.message.ifBlank { getString(R.string.card_form_failed) })
            }
        }
    }

    companion object {
        private const val PIN_LENGTH = 4
    }
}
