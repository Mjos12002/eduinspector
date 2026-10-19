package com.example.inspectorappupdate.ui.fragment.card

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.databinding.FragmentCardToolsBinding
import com.example.inspectorappupdate.model.card.CardModel
import com.example.inspectorappupdate.ui.fragment.issue.ReportCardFragment
import com.example.inspectorappupdate.ui.fragment.pin.ChangePinFragment
import com.example.inspectorappupdate.ui.fragment.topup.CardTopUpFragment
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// Card tab with top up, block card and change PIN. Each starts with tapping the student's card, then shows
// the student's details and that tab's form (embedded without its own header and student panel).
// Everything clears 2 s after the card is taken away from the phone.
class CardToolsFragment : Fragment() {

    private var _binding: FragmentCardToolsBinding? = null
    private val binding get() = _binding!!

    private val cardViewModel: CardViewModel by activityViewModels()

    private enum class Tool { TOPUP, BLOCK, PIN }

    private var current = Tool.TOPUP

    // Whether a student card was read since this screen opened; the forms show only after that.
    private var cardRead = false
    private var resetJob: Job? = null
    private var tapped = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) = hideChildChrome()
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) = hideChildChrome()
        }, false)

        binding.btnToolTopup.setOnClickListener { show(Tool.TOPUP) }
        binding.btnToolBlock.setOnClickListener { show(Tool.BLOCK) }
        binding.btnToolPin.setOnClickListener { show(Tool.PIN) }
        show(current)

        // Only a card tapped after this screen opened counts; the value held from before is ignored.
        val before = cardViewModel.cardLiveData.value
        cardViewModel.cardLiveData.observe(viewLifecycleOwner) { card ->
            if (card !== before && !cardViewModel.isNoCard(card)) renderCard(card)
            // The chosen screen also reacts to the card and may show its own student panel again.
            binding.root.post { hideChildChrome() }
        }

        cardViewModel.cardPresent.observe(viewLifecycleOwner) { present ->
            resetJob?.cancel()
            // Only a card tapped on this screen; an unknown card's message clears the same way.
            if (present) tapped = true
            if (present || !tapped) return@observe
            tapped = false
            resetJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(RESET_AFTER_MS)
                reset()
            }
        }
    }

    private fun renderCard(card: CardModel?) {
        val data = card?.data
        val ok = card != null && data != null && !card.error && data.reg_number.isNotBlank()
        binding.tvCardPrompt.visibility = if (ok) View.GONE else View.VISIBLE
        binding.llCardDetails.visibility = if (ok) View.VISIBLE else View.GONE
        cardRead = ok
        binding.flCardTool.visibility = if (ok) View.VISIBLE else View.GONE
        if (!ok) {
            // The embedded screens clear the card when they open (empty message); only a real lookup error is shown in red.
            val error = card?.message?.ifBlank { null }
            binding.tvCardPrompt.text = error ?: getString(R.string.permissions_tap_card)
            binding.tvCardPrompt.setTextColor(
                if (error != null) Color.parseColor("#C62828") else ContextCompat.getColor(requireContext(), R.color.card_details_text_color)
            )
            return
        }

        data!!
        binding.tvCardStudent.text = "${data.first_name} ${data.last_name}".trim()
        binding.tvCardMeta.text = "${data.reg_number} · card ${data.card_status.lowercase()}"
        binding.tvCardMeta.setTextColor(
            if (data.card_status.equals("active", true)) ContextCompat.getColor(requireContext(), R.color.text_color_hint)
            else Color.parseColor("#C62828")
        )
        binding.tvCardBalances.text = listOf(
            "Pocket money" to data.support_fees_balance,
            "Transport" to data.transport_fees_balance,
            "School fees" to data.school_fees_balance,
            "Insurance" to data.insurance_fees_balance
        ).joinToString("\n") { (label, amount) -> "$label: ${MONEY.format(amount)} RWF" }
    }

    // Back to "Tap the student's card" with an empty form.
    private fun reset() {
        if (_binding == null) return
        renderCard(null)
        childFragmentManager.beginTransaction().replace(R.id.fl_card_tool, freshTool(current)).commit()
    }

    private fun freshTool(tool: Tool): Fragment = when (tool) {
        Tool.TOPUP -> CardTopUpFragment()
        Tool.BLOCK -> ReportCardFragment()
        Tool.PIN -> ChangePinFragment()
    }

    private fun show(tool: Tool) {
        binding.flCardTool.visibility = if (cardRead) View.VISIBLE else View.GONE
        if (tool == current && childFragmentManager.findFragmentById(R.id.fl_card_tool) != null) return
        current = tool
        childFragmentManager.beginTransaction().replace(R.id.fl_card_tool, freshTool(tool)).commit()
        style()
    }

    // The details are shown above, so the embedded screen keeps only its action part.
    private fun hideChildChrome() {
        // The screen may have closed before this posted step ran.
        if (!isAdded || view == null) return
        val child = childFragmentManager.findFragmentById(R.id.fl_card_tool)?.view ?: return
        listOf(R.id.rl_header_title, R.id.tv_main_title, R.id.rl_student_details, R.id.rl_topup_student_details)
            .forEach { id -> child.findViewById<View>(id)?.visibility = View.GONE }
    }

    private fun style() {
        styleButton(binding.btnToolTopup, current == Tool.TOPUP)
        styleButton(binding.btnToolBlock, current == Tool.BLOCK)
        styleButton(binding.btnToolPin, current == Tool.PIN)
    }

    private fun styleButton(button: TextView, selected: Boolean) {
        button.setBackgroundColor(if (selected) ContextCompat.getColor(requireContext(), R.color.main_green_color) else Color.WHITE)
        button.setTextColor(if (selected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.card_details_text_color))
    }

    override fun onDestroyView() {
        resetJob?.cancel()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val MONEY = NumberFormat.getIntegerInstance(Locale.US)
        private const val RESET_AFTER_MS = 2000L
    }
}
