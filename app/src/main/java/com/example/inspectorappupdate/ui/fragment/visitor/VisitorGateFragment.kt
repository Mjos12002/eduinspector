package com.example.inspectorappupdate.ui.fragment.visitor

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.Toast
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.SearchStudentActivity.Companion.appDatabase
import com.example.inspectorappupdate.databinding.FragmentVisitorGateBinding
import com.example.inspectorappupdate.model.visitor.GateState
import com.example.inspectorappupdate.model.visitor.GateVisitor
import com.example.inspectorappupdate.model.visitor.RegisterStudent
import com.example.inspectorappupdate.model.visitor.VisitorCardItem
import com.example.inspectorappupdate.repository.visitor.VisitorResult
import com.example.inspectorappupdate.viewmodel.visitor.VisitorGateViewModel
import com.example.inspectorappupdate.viewmodel.visitor.VisitorGateViewModel.Direction
import com.example.inspectorappupdate.viewmodel.visitor.VisitorGateViewModel.Mode
import kotlinx.coroutines.launch

// Visitor cards: Gate shows the tapped card's student and visitors to check in or out; Attach links a
// printed card, found by student, visitor or code, to the tapped chip; Register adds a visitor.
class VisitorGateFragment : Fragment() {

    private var _binding: FragmentVisitorGateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VisitorGateViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVisitorGateBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnModeGate.setOnClickListener { viewModel.setMode(Mode.GATE) }
        binding.btnModeAttach.setOnClickListener { viewModel.setMode(Mode.ATTACH) }
        binding.btnModeRegister.setOnClickListener { viewModel.setMode(Mode.REGISTER) }
        setUpRegister()
        // Results update as the user types; the keyboard's search key runs it straight away.
        binding.btnCardSearch.visibility = View.GONE
        binding.etCardSearch.doAfterTextChanged { text ->
            if (viewModel.selectedCard.value == null) search(text?.toString().orEmpty(), immediate = false)
        }
        binding.etCardSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                search(binding.etCardSearch.text.toString(), immediate = true)
                true
            } else false
        }
        binding.btnAttachDone.setOnClickListener {
            viewModel.selectCard(null)
            binding.etCardSearch.setText("")
            binding.llSearchResults.removeAllViews()
        }

        viewModel.allowedModes.observe(viewLifecycleOwner) { modes ->
            binding.llModes.visibility = if (modes.size > 1) View.VISIBLE else View.GONE
            binding.btnModeGate.visibility = if (Mode.GATE in modes) View.VISIBLE else View.GONE
            binding.btnModeAttach.visibility = if (Mode.ATTACH in modes) View.VISIBLE else View.GONE
            binding.btnModeRegister.visibility = if (Mode.REGISTER in modes) View.VISIBLE else View.GONE
            // No gap before the first visible tab.
            listOf(binding.btnModeGate, binding.btnModeRegister, binding.btnModeAttach).forEachIndexed { index, button ->
                val first = listOf(binding.btnModeGate, binding.btnModeRegister, binding.btnModeAttach)
                    .first { it.visibility == View.VISIBLE } == button
                (button.layoutParams as ViewGroup.MarginLayoutParams).marginStart = if (first || index == 0) 0 else dp(8)
            }
        }

        viewModel.mode.observe(viewLifecycleOwner) { mode ->
            val gate = mode == Mode.GATE
            binding.tvScreenTitle.setText(if (gate) R.string.visitor_gate_title else R.string.visitor_cards_title)
            binding.tvScreenSubtitle.visibility = if (gate) View.GONE else View.VISIBLE
            binding.tvScreenSubtitle.setText(if (mode == Mode.REGISTER) R.string.visitor_register_subtitle else R.string.visitor_cards_subtitle)
            binding.llRegister.visibility = if (mode == Mode.REGISTER) View.VISIBLE else View.GONE
            styleModeButton(binding.btnModeRegister, mode == Mode.REGISTER)
            binding.llGate.visibility = if (gate) View.VISIBLE else View.GONE
            binding.llDirections.visibility = if (gate) View.VISIBLE else View.GONE
            binding.llAttach.visibility = if (mode == Mode.ATTACH) View.VISIBLE else View.GONE
            styleModeButton(binding.btnModeGate, gate)
            styleModeButton(binding.btnModeAttach, mode == Mode.ATTACH)
        }

        viewModel.busy.observe(viewLifecycleOwner) { busy ->
            binding.pbVisitor.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        }

        binding.btnCheckIn.setOnClickListener { viewModel.setDirection(Direction.IN) }
        binding.btnCheckOut.setOnClickListener { viewModel.setDirection(Direction.OUT) }
        viewModel.direction.observe(viewLifecycleOwner) { direction ->
            styleModeButton(binding.btnCheckIn, direction == Direction.IN)
            styleModeButton(binding.btnCheckOut, direction == Direction.OUT)
            renderGate()
        }

        binding.btnRecord.setOnClickListener { withToken { viewModel.record(it) } }
        viewModel.notice.observe(viewLifecycleOwner) { notice ->
            if (notice == null) return@observe
            Toast.makeText(requireContext(), notice, Toast.LENGTH_LONG).show()
            viewModel.noticeShown()
        }

        viewModel.gateState.observe(viewLifecycleOwner) { renderGate() }
        viewModel.selection.observe(viewLifecycleOwner) { renderGate() }
        viewModel.recordError.observe(viewLifecycleOwner) { error ->
            binding.tvRecordError.visibility = if (error != null) View.VISIBLE else View.GONE
            binding.tvRecordError.text = error.orEmpty()
        }
        viewModel.lookup.observe(viewLifecycleOwner) { result -> renderLookup(result) }
        viewModel.selectedCard.observe(viewLifecycleOwner) { card -> renderSelected(card) }
        viewModel.attachResult.observe(viewLifecycleOwner) { result -> renderAttach(result) }
        viewModel.cardInfo.observe(viewLifecycleOwner) { result -> renderCardInfo(result) }
    }

    private var token: String? = null

    private fun withToken(block: (String) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentToken = token ?: try {
                appDatabase.loggedInUserDao().getLastLogin().token.also { token = it }
            } catch (e: Exception) {
                return@launch
            }
            block(currentToken)
        }
    }

    private fun search(term: String, immediate: Boolean) = withToken { viewModel.search(it, term, immediate) }

    // ------------------------------------------------------------------ gate

    private fun renderGate() {
        val result = viewModel.gateState.value
        val data = (result as? VisitorResult.Success)?.data
        val leaving = viewModel.direction.value == Direction.OUT

        when {
            result == null -> showResult(
                getString(R.string.visitor_tap_prompt),
                getString(if (leaving) R.string.visitor_tap_hint_out else R.string.visitor_tap_hint),
                NEUTRAL
            )
            result is VisitorResult.Failure -> showResult("Couldn't read the card", result.message, REFUSED)
            data?.type == "student" -> showResult("Student card", "Record students on the In/Out screen.", NEUTRAL)
            leaving && data?.result == "accepted" && data.checkedOut.orEmpty().isEmpty() ->
                showResult("NO ONE INSIDE", data.message, NEUTRAL)
            leaving && data?.result == "accepted" -> showResult("CHECKED OUT", recordedSummary(data), EXIT)
            data?.authorized != true -> showResult("NOT ALLOWED", data?.message ?: "Access refused", REFUSED)
            data.result == "accepted" -> showResult("RECORDED", recordedSummary(data), ENTRY)
            else -> showResult(null, null, NEUTRAL)
        }

        val student = data?.student
        val showStudent = student != null && data.type == "visitor"
        binding.tvStudentLabel.visibility = if (showStudent) View.VISIBLE else View.GONE
        binding.tvStudentName.visibility = if (showStudent) View.VISIBLE else View.GONE
        binding.tvStudentName.text = student?.name.orEmpty()

        // Check in lists every visitor, those inside greyed out. Check out is recorded on the tap itself.
        val visitors = if (!leaving && data?.authorized == true) data.visitors.orEmpty() else emptyList()
        val selection = viewModel.selection.value.orEmpty()
        binding.tvVisitorsLabel.visibility = if (visitors.isNotEmpty()) View.VISIBLE else View.GONE
        binding.llVisitors.removeAllViews()
        visitors.forEach { binding.llVisitors.addView(visitorRow(it, it.id in selection)) }

        binding.btnRecord.visibility = if (visitors.isNotEmpty()) View.VISIBLE else View.GONE
        styleRecordButton(visitors.filter { it.id in selection })
        if (result == null) binding.tvRecordError.visibility = View.GONE
    }

    private fun visitorRow(visitor: GateVisitor, checked: Boolean): View {
        val inside = visitor.inside == true
        val allowed = visitor.isEnabled == true
        val since = "inside since ${clock(visitor.checkInTime.orEmpty())}"
        val detail = when {
            inside -> "${visitor.relationship.orEmpty()} · already $since"
            !allowed -> "${visitor.relationship.orEmpty()} · not allowed to visit"
            else -> visitor.relationship.orEmpty()
        }
        val selectable = allowed && !inside

        return CheckBox(requireContext()).apply {
            text = "${visitor.fullName.orEmpty()}\n$detail"
            textSize = 16f
            // Someone already inside shows ticked; they can't be checked in again.
            isChecked = checked || inside
            isEnabled = selectable
            setTextColor(
                if (selectable) ContextCompat.getColor(requireContext(), R.color.card_details_text_color)
                else Color.parseColor("#9E9E9E")
            )
            buttonTintList = ColorStateList.valueOf(ENTRY)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener { viewModel.toggleVisitor(visitor.id) }
        }
    }

    private fun styleRecordButton(ticked: List<GateVisitor>) {
        val count = ticked.size
        binding.btnRecord.isEnabled = count > 0
        binding.btnRecord.setText(R.string.visitor_confirm)
        binding.btnRecord.backgroundTintList = ColorStateList.valueOf(if (count == 0) Color.parseColor("#9E9E9E") else ENTRY)
    }

    private fun recordedSummary(data: GateState): String = listOfNotNull(
        data.message,
        data.checkedIn.orEmpty().takeIf { it.isNotEmpty() }?.let { "In: ${it.joinToString(", ")}" },
        data.checkedOut.orEmpty().takeIf { it.isNotEmpty() }?.let { "Out: ${it.joinToString(", ")}" }
    ).joinToString("\n")

    // A null status hides the status line (a usable card shows just the student and visitors).
    private fun showResult(status: String?, message: String?, tone: Int) {
        binding.tvResultStatus.visibility = if (status != null) View.VISIBLE else View.GONE
        binding.tvResultMessage.visibility = if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.tvResultStatus.text = status.orEmpty()
        binding.tvResultStatus.setTextColor(tone)
        binding.tvResultMessage.text = message.orEmpty()
        tintPanel(binding.llResult, tone)
    }

    // ------------------------------------------------------------------ register visitor

    private val relationships = listOf(
        "Father", "Mother", "Guardian", "Brother", "Sister", "Uncle", "Aunt",
        "Grandfather", "Grandmother", "Cousin", "Sponsor", "Family Friend", "Other"
    )
    private var relationship: String? = null

    private fun setUpRegister() {
        relationships.forEach { name ->
            val pill = TextView(requireContext()).apply {
                text = name
                textSize = 15f
                setPadding(dp(14), dp(9), dp(14), dp(9))
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginEnd = dp(8) }
                setOnClickListener { relationship = name; renderRelationships() }
            }
            binding.llRelationships.addView(pill)
        }
        renderRelationships()

        binding.btnRegisterSave.setOnClickListener {
            val name = binding.etVisitorName.text.toString().trim()
            val phone = binding.etVisitorPhone.text.toString().trim()
            val error = when {
                name.length < 3 -> "Enter the visitor's full name"
                relationship == null -> "Choose how they are related to the student"
                else -> null
            }
            binding.tvRegisterError.visibility = if (error != null) View.VISIBLE else View.GONE
            binding.tvRegisterError.text = error.orEmpty()
            if (error == null) withToken { viewModel.registerVisitor(it, name, phone.ifEmpty { null }, relationship!!) }
        }
        binding.btnRegisterCancel.setOnClickListener { viewModel.clearRegister() }

        viewModel.registerStudent.observe(viewLifecycleOwner) { renderRegisterStudent(it) }
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                null -> return@observe
                is VisitorResult.Success -> {
                    Toast.makeText(requireContext(), "Visitor saved. Their card is waiting to be attached.", Toast.LENGTH_LONG).show()
                    clearRegisterForm()
                }
                is VisitorResult.Failure -> {
                    binding.tvRegisterError.text = result.message
                    binding.tvRegisterError.visibility = View.VISIBLE
                }
            }
            viewModel.registerResultShown()
        }
    }

    private fun renderRegisterStudent(result: VisitorResult<RegisterStudent>?) {
        val student = (result as? VisitorResult.Success)?.data
        binding.tvRegisterPrompt.visibility = if (student == null) View.VISIBLE else View.GONE
        binding.llRegisterForm.visibility = if (student == null) View.GONE else View.VISIBLE
        binding.tvRegisterPrompt.text = (result as? VisitorResult.Failure)?.message ?: getString(R.string.permissions_tap_card)
        binding.tvRegisterPrompt.setTextColor(if (result is VisitorResult.Failure) REFUSED else NEUTRAL)
        if (student == null) {
            clearRegisterForm()
            return
        }

        binding.tvRegisterStudent.text = student.name.orEmpty()
        binding.tvRegisterStudentMeta.text = listOfNotNull(
            student.regNumber,
            student.visitorCard?.let { "visitor card ${it.cardCode} · ${if (it.status == "pending") "waiting for a card" else it.status}" }
                ?: "no visitor card yet"
        ).joinToString(" · ")
        val visitors = student.visitors.orEmpty()
        binding.tvRegisterVisitors.text = if (visitors.isEmpty()) "No one yet" else
            visitors.joinToString("\n") { "• ${it.fullName.orEmpty()} — ${it.relationship.orEmpty()}" + if (it.isEnabled == false) " (not allowed)" else "" }
    }

    private fun clearRegisterForm() {
        binding.etVisitorName.setText("")
        binding.etVisitorPhone.setText("")
        binding.tvRegisterError.visibility = View.GONE
        relationship = null
        renderRelationships()
    }

    private fun renderRelationships() {
        for (i in 0 until binding.llRelationships.childCount) {
            val pill = binding.llRelationships.getChildAt(i) as TextView
            val selected = pill.text == relationship
            val green = ContextCompat.getColor(requireContext(), R.color.main_green_color)
            pill.background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(if (selected) green else Color.WHITE)
                setStroke(dp(1), if (selected) green else Color.parseColor("#C8C8C8"))
            }
            pill.setTextColor(if (selected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.card_details_text_color))
        }
    }

    // ------------------------------------------------------------------ attach

    private fun renderLookup(result: VisitorResult<List<VisitorCardItem>>?) {
        binding.llSearchResults.removeAllViews()
        binding.tvSearchMessage.visibility = View.GONE
        if (result == null || viewModel.selectedCard.value != null) return

        when (result) {
            is VisitorResult.Failure -> showSearchMessage(result.message)
            is VisitorResult.Success -> {
                if (result.data.isEmpty()) {
                    showSearchMessage("No visitor card matches that student, visitor or code.")
                    return
                }
                result.data.forEach { card -> binding.llSearchResults.addView(cardRow(card)) }
            }
        }
    }

    private fun cardRow(card: VisitorCardItem): View {
        val row = TextView(requireContext())
        val visitors = card.visitors.orEmpty().joinToString(", ") { "${it.fullName} (${it.relationship})" }
        val state = when {
            card.status == "inactive" -> "disabled"
            card.chipAttached == true -> "has a card — tapping replaces it"
            else -> "waiting for a card"
        }
        row.text = "${card.student?.name.orEmpty()}\n${card.cardCode.orEmpty()} · $state\nVisitors: $visitors"
        row.textSize = 15f
        row.setTextColor(ContextCompat.getColor(requireContext(), R.color.card_details_text_color))
        row.setPadding(dp(14), dp(12), dp(14), dp(12))
        row.background = ContextCompat.getDrawable(requireContext(), R.drawable.visitor_panel_background)
        row.layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) }
        row.setOnClickListener { viewModel.selectCard(card) }
        return row
    }

    private fun renderSelected(card: VisitorCardItem?) {
        val selected = card != null
        binding.llSelectedCard.visibility = if (selected) View.VISIBLE else View.GONE
        binding.llSearch.visibility = if (selected) View.GONE else View.VISIBLE
        binding.llSearchResults.visibility = if (selected) View.GONE else View.VISIBLE
        binding.tvAttachHint.visibility = if (selected) View.GONE else View.VISIBLE
        binding.btnAttachDone.visibility = View.GONE
        if (card == null) return

        binding.tvAttachStatus.text = getString(R.string.visitor_attach_prompt)
        binding.tvAttachStatus.setTextColor(NEUTRAL)
        tintPanel(binding.llSelectedCard, NEUTRAL)
        binding.tvAttachCard.text = "${card.student?.name.orEmpty()}\n${card.cardCode.orEmpty()}"
        binding.tvAttachMessage.visibility = View.GONE
    }

    private fun renderAttach(result: VisitorResult<VisitorCardItem>?) {
        val selected = viewModel.selectedCard.value ?: return
        // Cleared after a failed attach: the chosen card waits for another tap.
        if (result == null) {
            renderSelected(selected)
            return
        }

        binding.tvAttachMessage.visibility = View.VISIBLE
        when (result) {
            is VisitorResult.Success -> {
                binding.tvAttachStatus.text = "CARD ACTIVATED"
                binding.tvAttachStatus.setTextColor(ENTRY)
                tintPanel(binding.llSelectedCard, ENTRY)
                binding.tvAttachMessage.text = "${result.data.cardCode} can now be used at the gate."
                binding.tvAttachMessage.setTextColor(ENTRY)
                binding.btnAttachDone.visibility = View.VISIBLE
            }
            is VisitorResult.Failure -> {
                binding.tvAttachStatus.text = "Not attached"
                binding.tvAttachStatus.setTextColor(REFUSED)
                binding.tvAttachMessage.text = "${result.message}\nTap another blank card, or go back."
                binding.tvAttachMessage.setTextColor(REFUSED)
                binding.btnAttachDone.visibility = View.VISIBLE
            }
        }
    }

    private fun renderCardInfo(result: VisitorResult<GateState>?) {
        if (result == null) {
            binding.llCardInfo.visibility = View.GONE
            return
        }
        binding.llCardInfo.visibility = View.VISIBLE
        binding.tvCardInfoVisitor.visibility = View.GONE
        binding.tvCardInfoStudents.visibility = View.GONE

        val info = (result as? VisitorResult.Success)?.data
        if (info == null) {
            showCardInfo("Couldn't read this card", (result as? VisitorResult.Failure)?.message, REFUSED)
            return
        }

        val student = info.student
        when {
            info.type == "student" -> showCardInfo("Student card", "This is a student's card. It can't be used as a visitor card.", REFUSED)
            info.type != "visitor" -> showCardInfo(
                "Blank card",
                "Not linked to any student. Search for the student above, then tap this card.",
                NEUTRAL
            )
            info.reason == "other_school" -> showCardInfo("Another school's card", info.message, REFUSED)
            else -> {
                val status = info.card?.status.orEmpty()
                val active = status == "active"
                showCardInfo(if (active) "Registered card" else "Registered card · ${status.uppercase()}", null, if (active) ENTRY else REFUSED)
                binding.tvCardInfoVisitor.visibility = View.VISIBLE
                binding.tvCardInfoVisitor.text = listOfNotNull(
                    student?.name,
                    listOfNotNull(info.card?.cardCode, student?.regNumber).joinToString(" · ").ifBlank { null },
                    if (info.authorized != true && active) info.message else null
                ).joinToString("\n")

                val visitors = info.visitors.orEmpty()
                binding.tvCardInfoStudents.visibility = View.VISIBLE
                binding.tvCardInfoStudents.text = if (visitors.isEmpty()) "No authorized visitors" else
                    "Authorized visitors:\n" + visitors.joinToString("\n") { v ->
                        val notes = listOfNotNull(
                            if (v.isEnabled == false) "not allowed" else null,
                            if (v.inside == true) "inside" else null
                        )
                        "• ${v.fullName.orEmpty()} — ${v.relationship.orEmpty()}" +
                            if (notes.isEmpty()) "" else " (${notes.joinToString(", ")})"
                    }
            }
        }
    }

    private fun showCardInfo(title: String, detail: String?, color: Int) {
        binding.tvCardInfoTitle.text = title
        binding.tvCardInfoTitle.setTextColor(color)
        tintPanel(binding.llCardInfo, color)
        if (detail != null) {
            binding.tvCardInfoVisitor.visibility = View.VISIBLE
            binding.tvCardInfoVisitor.text = detail
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun showSearchMessage(message: String) {
        binding.tvSearchMessage.text = message
        binding.tvSearchMessage.visibility = View.VISIBLE
    }

    private fun styleModeButton(button: TextView, selected: Boolean) {
        button.setBackgroundColor(if (selected) ContextCompat.getColor(requireContext(), R.color.main_green_color) else Color.WHITE)
        button.setTextColor(if (selected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.card_details_text_color))
    }

    private fun tintPanel(view: View, tone: Int) {
        val panel = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (tone == NEUTRAL) Color.WHITE else adjustAlpha(tone, 0.08f))
            setStroke(dp(if (tone == NEUTRAL) 1 else 2), if (tone == NEUTRAL) Color.parseColor("#D1D1D1") else tone)
        }
        view.background = panel
    }

    private fun adjustAlpha(color: Int, factor: Float): Int =
        Color.argb((255 * factor).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    // The API sends ISO-8601 times in the school's timezone, e.g. 2026-10-01T07:26:00+02:00.
    private fun clock(iso: String): String =
        Regex("T(\\d{2}:\\d{2})").find(iso)?.groupValues?.get(1) ?: iso

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etCardSearch.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val ENTRY = Color.parseColor("#01865A")
        private val EXIT = Color.parseColor("#014746")
        private val REFUSED = Color.parseColor("#C62828")
        private val NEUTRAL = Color.parseColor("#002424")
    }
}
