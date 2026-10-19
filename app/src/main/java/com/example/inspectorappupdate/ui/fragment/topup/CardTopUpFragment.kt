package com.example.inspectorappupdate.ui.fragment.topup

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
import com.example.inspectorappupdate.apirequest.dto.payment.PaymentDTO
import com.example.inspectorappupdate.databinding.FragmentCardTopUpBinding
import com.example.inspectorappupdate.model.card.CardModelData
import com.example.inspectorappupdate.model.payment.PaymentTypeModelData
import com.example.inspectorappupdate.model.wallet.WalletModelData
import com.example.inspectorappupdate.repository.payment.PaymentRepository
import com.example.inspectorappupdate.repository.student.StudentRepository
import com.example.inspectorappupdate.ui.fragment.card.CardFormUi
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.utils.Keyboard
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import com.example.inspectorappupdate.viewmodel.payment.PaymentViewModel
import com.example.inspectorappupdate.viewmodel.wallet.WalletViewModel
import kotlinx.coroutines.launch

/** Top up form of the Cards tab: wallet, payment method, paying phone and amount for the tapped card. */
class CardTopUpFragment : Fragment() {

    private var _binding: FragmentCardTopUpBinding? = null
    private val binding get() = _binding!!

    private val cardViewModel: CardViewModel by activityViewModels()
    private val walletViewModel: WalletViewModel by activityViewModels()
    private val paymentViewModel: PaymentViewModel by activityViewModels()

    private var wallets: List<WalletModelData> = emptyList()
    private var paymentTypes: List<PaymentTypeModelData> = emptyList()
    private var wallet = 0
    private var paymentType = 0

    private var card: CardModelData? = null
    // The student record and main parent the top-up is credited to.
    private var studentID = 0
    private var parentID = 0
    private var sending = false
    private var sent = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardTopUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pocket money and transport only; the other wallets are paid elsewhere.
        walletViewModel.walletLiveData.observe(viewLifecycleOwner) { res ->
            wallets = res?.data.orEmpty().filter { it.id > 0 && it.name.lowercase().let { n -> "pocket" in n || "transport" in n } }
            renderWallets()
        }
        paymentViewModel.paymentLiveData.observe(viewLifecycleOwner) { res ->
            paymentTypes = res?.data.orEmpty().filter { it.id > 0 }
            renderPaymentTypes()
        }
        withToken {
            walletViewModel.getWallet(it)
            paymentViewModel.getPaymentType(it)
        }

        cardViewModel.cardLiveData.observe(viewLifecycleOwner) { model ->
            if (cardViewModel.isNoCard(model) || model.error || model.data == null) return@observe
            if (model.data.card_number == card?.card_number) return@observe
            card = model.data
            studentID = 0
            parentID = 0
            sent = false
            binding.etAmount.text?.clear()
            CardFormUi.result(binding.tvResult, null, ok = true)
            loadStudent(model.data.reg_number)
            updateButton()
        }

        binding.btnTopup.setOnClickListener { send() }
        updateButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderWallets() {
        if (_binding == null) return
        if (wallet >= wallets.size) wallet = 0
        CardFormUi.pills(binding.llWallets, wallets.map { label(it.details, it.name) }, wallet) {
            wallet = it
            renderWallets()
        }
    }

    private fun renderPaymentTypes() {
        if (_binding == null) return
        if (paymentType >= paymentTypes.size) paymentType = 0
        CardFormUi.pills(binding.llPaymentTypes, paymentTypes.map { label(it.details, it.name) }, paymentType) {
            paymentType = it
            renderPaymentTypes()
        }
    }

    private fun label(details: String?, name: String) = details?.takeIf { it.isNotBlank() } ?: name

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadStudent(regNumber: String) = withToken { token ->
        val res = StudentRepository().getStudentDetails(token, regNumber)
        val student = res.data?.student
        // Dropped when another card was tapped meanwhile.
        if (_binding == null || card?.reg_number != regNumber || student == null) return@withToken
        studentID = student.id
        parentID = student.parent_user_id
        updateButton()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun send() {
        val current = card ?: return
        val phone = normalizePhone(binding.etPhoneNumber.text.toString())
        val amount = binding.etAmount.text.toString().trim().toIntOrNull()
        if (phone == null) {
            CardFormUi.result(binding.tvResult, getString(R.string.topup_bad_phone), ok = false)
            return
        }
        if (amount == null || amount <= 0) {
            CardFormUi.result(binding.tvResult, getString(R.string.topup_bad_amount), ok = false)
            return
        }
        val walletType = wallets.getOrNull(wallet) ?: return
        val payment = paymentTypes.getOrNull(paymentType) ?: return
        Keyboard.hide(requireActivity())
        sending = true
        binding.btnTopup.text = getString(R.string.topup_sending)
        CardFormUi.result(binding.tvResult, null, ok = true)
        updateButton()
        withToken { token ->
            val dto = PaymentDTO(amount, walletType.id, payment.id, phone, studentID, parentID, current.card_number)
            val res = PaymentRepository().makePayment(token, dto)
            sending = false
            if (_binding == null) return@withToken
            binding.btnTopup.text = getString(R.string.topup)
            if (res.status == 200 && !res.error) {
                // One request per tap, so a second press can't send a second payment prompt.
                sent = true
                CardFormUi.result(binding.tvResult, getString(R.string.topup_sent, phone), ok = true)
            } else {
                CardFormUi.result(binding.tvResult, res.message.ifBlank { getString(R.string.card_form_failed) }, ok = false)
            }
            updateButton()
        }
    }

    private fun updateButton() {
        if (_binding == null) return
        val ready = card != null && studentID > 0 && wallets.isNotEmpty() && paymentTypes.isNotEmpty() && !sending && !sent
        CardFormUi.styleButton(binding.btnTopup, ready)
    }

    // 07XXXXXXXX from +2507…, 2507…, 7… with spaces or dashes; null when it isn't a Rwandan mobile number.
    private fun normalizePhone(input: String): String? {
        var digits = input.filter { it.isDigit() }
        if (digits.startsWith("250")) digits = digits.drop(3)
        if (digits.length == 9 && digits.startsWith("7")) digits = "0$digits"
        return digits.takeIf { Regex("^07\\d{8}$").matches(it) }
    }

    private fun withToken(block: suspend (String) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = DbUtility().dbBuilder(requireContext()).loggedInUserDao().getLastLogin().token
            block(token)
        }
    }
}
