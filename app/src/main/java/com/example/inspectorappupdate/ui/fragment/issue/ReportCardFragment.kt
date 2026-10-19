package com.example.inspectorappupdate.ui.fragment.issue

import android.app.AlertDialog
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
import com.example.inspectorappupdate.databinding.FragmentReportCardBinding
import com.example.inspectorappupdate.model.card.CardModelData
import com.example.inspectorappupdate.repository.card.CardRepository
import com.example.inspectorappupdate.ui.fragment.card.CardFormUi
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import kotlinx.coroutines.launch

/** Block card form of the Cards tab: pick the reason, confirm, and the tapped card stops working. */
class ReportCardFragment : Fragment() {

    private var _binding: FragmentReportCardBinding? = null
    private val binding get() = _binding!!

    private val cardViewModel: CardViewModel by activityViewModels()

    private val reasons = listOf(
        "lost" to "Lost", "stolen" to "Stolen", "damaged" to "Damaged",
        "replaced" to "Replaced", "left-school" to "Left school", "other" to "Other"
    )
    private var reason = 0
    private var card: CardModelData? = null
    private var blocking = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnReport.setOnClickListener { confirm() }
        cardViewModel.cardLiveData.observe(viewLifecycleOwner) { model ->
            if (cardViewModel.isNoCard(model) || model.error || model.data == null) return@observe
            card = model.data
            reason = 0
            render()
        }
        render()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(message: String? = null, ok: Boolean = false) {
        if (_binding == null) return
        val active = card?.card_status.equals("active", ignoreCase = true)
        binding.llBlockForm.visibility = if (active) View.VISIBLE else View.GONE
        binding.btnReport.visibility = if (active) View.VISIBLE else View.GONE
        CardFormUi.pills(binding.llReasons, reasons.map { it.second }, reason) {
            reason = it
            render()
        }
        CardFormUi.styleButton(binding.btnReport, card != null && !blocking, danger = true)
        val notActive = card?.takeIf { !active }?.let { getString(R.string.block_not_active, it.card_status.lowercase()) }
        CardFormUi.result(binding.tvResult, message ?: notActive, ok = ok)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun confirm() {
        val current = card ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Block ${current.first_name} ${current.last_name}'s card?")
            .setMessage("Reason: ${reasons[reason].second}. ${getString(R.string.block_note)}")
            .setPositiveButton(R.string.block_button) { _, _ -> block(current) }
            .setNegativeButton("Keep it", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun block(current: CardModelData) {
        blocking = true
        binding.btnReport.text = getString(R.string.block_blocking)
        render()
        viewLifecycleOwner.lifecycleScope.launch {
            val token = DbUtility().dbBuilder(requireContext()).loggedInUserDao().getLastLogin().token
            val res = CardRepository().disableCard(current.indicator, token, reasons[reason].first)
            blocking = false
            if (_binding == null) return@launch
            binding.btnReport.text = getString(R.string.block_button)
            if (res.error) {
                render(res.message.ifBlank { getString(R.string.card_form_failed) })
            } else {
                card = current.copy(card_status = "blocked")
                render()
                CardFormUi.result(binding.tvResult, getString(R.string.block_done), ok = true)
            }
        }
    }
}
