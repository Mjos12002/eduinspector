package com.example.inspectorappupdate.ui.fragment.agent

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.SearchStudentActivity.Companion.appDatabase
import com.example.inspectorappupdate.databinding.FragmentCardIssueBinding
import com.example.inspectorappupdate.model.agent.AgentStudent
import com.example.inspectorappupdate.model.agent.CardFee
import com.example.inspectorappupdate.repository.agent.AgentResult
import com.example.inspectorappupdate.utils.signOut
import com.example.inspectorappupdate.viewmodel.agent.CardIssueViewModel
import com.example.inspectorappupdate.viewmodel.agent.CardIssueViewModel.Step
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

// A card agent hands a card to a student: search, check class and gender, block the old card,
// tap the new card, the student types the PIN twice on the keypad, then activate.
@RequiresApi(Build.VERSION_CODES.O)
class CardIssueFragment : Fragment() {

    private var _binding: FragmentCardIssueBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CardIssueViewModel by activityViewModels()

    private val genders = listOf("male" to "Male", "female" to "Female")
    private var selectedGender: String? = null
    private var selectedClassId: Int? = null
    private var token: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardIssueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgLogout.setOnClickListener { signOut() }
        binding.imgBack.setOnClickListener { goBack() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.back()) {
                    // Nothing to step back to: let the screen itself go back.
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                } else {
                    afterBack()
                }
            }
        })

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                withToken { viewModel.search(it, s?.toString().orEmpty()) }
            }
        })
        binding.etSearch.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) hideKeyboard()
            false
        }

        buildGenderPills()
        binding.btnSave.setOnClickListener { withToken { viewModel.saveDetails(it, selectedGender, selectedClassId) } }
        binding.btnBlock.setOnClickListener { confirmBlock() }
        binding.btnRegister.setOnClickListener { viewModel.startRegistration() }
        binding.btnActivate.setOnClickListener { withToken { viewModel.activate(it) } }
        binding.btnNext.setOnClickListener {
            binding.etSearch.setText("")
            viewModel.nextStudent()
        }
        buildKeypad()
        styleButton(binding.btnSave, primary = false)
        styleButton(binding.btnRegister, primary = true)
        styleButton(binding.btnActivate, primary = true)
        styleButton(binding.btnNext, primary = true)
        styleButton(binding.btnBlock, primary = false, danger = true)

        viewModel.step.observe(viewLifecycleOwner) { renderStep(it) }
        viewModel.busy.observe(viewLifecycleOwner) { busy ->
            binding.pbIssue.visibility = if (busy) View.VISIBLE else View.INVISIBLE
            binding.btnActivate.isEnabled = !busy
        }
        viewModel.results.observe(viewLifecycleOwner) { renderResults(it) }
        viewModel.student.observe(viewLifecycleOwner) { renderStudent(it) }
        viewModel.stepError.observe(viewLifecycleOwner) { error ->
            binding.tvStepError.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.tvStepError.text = error.orEmpty()
        }
        viewModel.pinDigits.observe(viewLifecycleOwner) { renderPin() }
        viewModel.pinRepeat.observe(viewLifecycleOwner) { renderPin() }
        viewModel.issued.observe(viewLifecycleOwner) { issued ->
            val student = viewModel.student.value
            binding.tvDoneTitle.text = getString(R.string.issue_done_title)
            binding.tvDone.text = listOfNotNull(
                student?.name,
                student?.studentCode?.let { "Student code $it" },
                issued?.card?.cardNumber?.let { "Card ${tail(it)}" },
                issued?.card?.validTo?.let { "Valid until ${it.take(10)}" },
                if (issued?.repeated == true) "This card was already active; nothing changed." else "Issued by you, now.",
            ).joinToString("\n")
        }
        viewModel.notice.observe(viewLifecycleOwner) { message ->
            if (message == null) return@observe
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            viewModel.noticeShown()
        }
    }

    private fun renderStep(step: Step) {
        val searching = step == Step.SEARCH
        binding.llSearch.visibility = if (searching) View.VISIBLE else View.GONE
        binding.svSteps.visibility = if (searching) View.GONE else View.VISIBLE
        binding.imgBack.visibility = if (searching) View.INVISIBLE else View.VISIBLE
        binding.llStudent.visibility = if (step == Step.STUDENT) View.VISIBLE else View.GONE
        binding.llTap.visibility = if (step == Step.TAP) View.VISIBLE else View.GONE
        binding.llPin.visibility = if (step == Step.PIN) View.VISIBLE else View.GONE
        binding.llConfirm.visibility = if (step == Step.CONFIRM) View.VISIBLE else View.GONE
        binding.llDone.visibility = if (step == Step.DONE) View.VISIBLE else View.GONE
        binding.tvStep.text = when (step) {
            Step.SEARCH -> "Find the student"
            Step.STUDENT -> "Check details and card"
            Step.TAP -> "Step 1 of 3 · new card"
            Step.PIN -> "Step 2 of 3 · student PIN"
            Step.CONFIRM -> "Step 3 of 3 · activate"
            Step.DONE -> "Done"
        }
        if (step == Step.CONFIRM) renderConfirm()
        if (!searching) hideKeyboard()
    }

    private fun renderResults(result: AgentResult<List<AgentStudent>>?) {
        binding.llResults.removeAllViews()
        when (result) {
            null -> binding.tvSearchStatus.text = getString(R.string.issue_search_prompt)
            is AgentResult.Failure -> binding.tvSearchStatus.text = result.message
            is AgentResult.Success -> {
                binding.tvSearchStatus.text = when (result.data.size) {
                    0 -> "No student found at your schools."
                    1 -> "1 student"
                    else -> "${result.data.size} students"
                }
                result.data.forEach { binding.llResults.addView(resultRow(it)) }
            }
        }
    }

    private fun resultRow(student: AgentStudent): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(requireContext(), R.drawable.visitor_panel_background)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(8) }
            setOnClickListener { withToken { viewModel.openStudent(it, student.id) } }
        }
        val top = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(text(student.name.orEmpty(), 16f, DARK, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        top.addView(badge(if (student.hasActiveCard) "Active card" else "No card", if (student.hasActiveCard) ACTIVE else NEW))
        row.addView(top)
        row.addView(text(listOfNotNull(student.studentCode, student.className, student.instituteName).joinToString(" · "), 13f, MUTED))
        if (student.print?.withMe == true) row.addView(text("Printed card is with you", 13f, ACTIVE))
        return row
    }

    private fun renderStudent(student: AgentStudent?) {
        if (student == null) return
        binding.tvName.text = student.name
        binding.tvMeta.text = listOfNotNull(
            student.studentCode?.let { "Code $it" },
            student.className ?: "No class",
            student.instituteName
        ).joinToString(" · ")
        if (student.photoUrl != null) {
            Picasso.get().load(student.photoUrl).fit().centerCrop().into(binding.imgPhoto)
        } else {
            binding.imgPhoto.setImageDrawable(null)
        }

        selectedGender = student.gender?.lowercase()
        selectedClassId = student.classId
        renderGender()
        renderClasses(student)
        updateSaveButton()

        val card = student.activeCard
        if (card != null) {
            binding.tvCardState.text = "Active card"
            binding.tvCardState.setTextColor(ACTIVE)
            binding.tvCardMeta.text = listOfNotNull(
                card.cardNumber?.let { "Card ${tail(it)}" },
                card.validTo?.let { "Valid until ${it.take(10)}" },
                "To give a new card, block this one first."
            ).joinToString("\n")
        } else {
            binding.tvCardState.text = "No active card"
            binding.tvCardState.setTextColor(NEW)
            binding.tvCardMeta.text = when {
                student.print?.withMe == true -> "The printed card is with you. Register it to activate it."
                student.print?.status == "printed" && student.print.heldByAgent -> "The printed card is with another agent."
                student.print != null -> "The card is ${student.print.status?.replace('_', ' ')} at the office."
                else -> "No printed card on record. Any new Educard card can be registered."
            }
        }
        binding.btnBlock.visibility = if (card != null) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = card == null
        styleButton(binding.btnRegister, primary = card == null)
    }

    private fun renderClasses(student: AgentStudent) {
        val classes = student.classes.orEmpty()
        val first = when {
            classes.isEmpty() -> "No classes set up at this school"
            student.classId == null -> "Choose a class"
            else -> student.className ?: "—"
        }
        val labels = listOf(first) + classes.map { it.name }
        binding.spClass.adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                (super.getView(position, convertView, parent) as TextView).apply { setTextColor(if (position == 0 && student.classId == null) MUTED else DARK) }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                (super.getDropDownView(position, convertView, parent) as TextView).apply {
                    setTextColor(DARK)
                    setBackgroundColor(Color.WHITE)
                }
        }
        binding.spClass.setSelection(classes.indexOfFirst { it.id == student.classId } + 1)
        binding.spClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedClassId = if (position == 0) student.classId else classes[position - 1].id
                updateSaveButton()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.spClass.isEnabled = classes.isNotEmpty()
    }

    private fun buildGenderPills() {
        binding.llGender.removeAllViews()
        genders.forEach { (value, label) ->
            binding.llGender.addView(TextView(requireContext()).apply {
                text = label
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(10), dp(18), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) }
                setOnClickListener {
                    selectedGender = value
                    renderGender()
                    updateSaveButton()
                }
            })
        }
    }

    private fun renderGender() {
        genders.forEachIndexed { index, (value, _) ->
            stylePill(binding.llGender.getChildAt(index) as TextView, value == selectedGender)
        }
    }

    private fun updateSaveButton() {
        val student = viewModel.student.value ?: return
        val changed = (selectedGender != null && selectedGender != student.gender?.lowercase()) ||
            (selectedClassId != null && selectedClassId != student.classId)
        binding.btnSave.isEnabled = changed
        styleButton(binding.btnSave, primary = changed)
    }

    private fun confirmBlock() {
        val reasons = listOf("lost" to "Lost", "stolen" to "Stolen", "damaged" to "Damaged", "replaced" to "Replaced by a new card", "other" to "Other")
        var chosen = 0
        AlertDialog.Builder(requireContext())
            .setTitle("Block ${tail(viewModel.student.value?.activeCard?.cardNumber.orEmpty())}?")
            .setSingleChoiceItems(reasons.map { it.second }.toTypedArray(), chosen) { _, which -> chosen = which }
            .setPositiveButton("Block card") { _, _ -> withToken { viewModel.blockCard(it, reasons[chosen].first) } }
            .setNegativeButton("Keep it", null)
            .show()
    }

    private fun buildKeypad() {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        binding.glKeypad.removeAllViews()
        keys.forEach { key ->
            binding.glKeypad.addView(TextView(requireContext()).apply {
                text = key
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(DARK)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dp(76)
                    height = dp(60)
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
                if (key.isNotEmpty()) {
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(Color.parseColor("#F4F4F4"))
                    }
                    contentDescription = if (key == "⌫") "Delete" else key
                    setOnClickListener { if (key == "⌫") viewModel.deleteDigit() else viewModel.typeDigit(key[0]) }
                }
            })
        }
    }

    private fun renderPin() {
        val repeat = viewModel.pinRepeat.value == true
        binding.tvPinTitle.text = getString(if (repeat) R.string.issue_pin_again else R.string.issue_pin_first)
        binding.tvPinHint.text = getString(R.string.issue_pin_hint)
        val typed = viewModel.pinDigits.value ?: 0
        binding.llPinDots.removeAllViews()
        repeat(CardIssueViewModel.PIN_LENGTH) { index ->
            binding.llPinDots.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { setMargins(dp(8), 0, dp(8), 0) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (index < typed) DARK else Color.TRANSPARENT)
                    setStroke(dp(2), DARK)
                }
            })
        }
    }

    private fun renderConfirm() {
        val student = viewModel.student.value
        binding.tvConfirm.text = listOfNotNull(
            "Student: ${student?.name.orEmpty()}",
            student?.studentCode?.let { "Code: $it" },
            student?.className?.let { "Class: $it" },
            "New card: ${tail(viewModel.tappedUid.orEmpty())}",
            "PIN: chosen by the student",
            viewModel.fee?.let { feeLine(it) },
            "The card will be active straight away and listed as issued by you."
        ).joinToString("\n")
    }

    private fun feeLine(fee: CardFee): String {
        val amount = "RWF " + java.text.NumberFormat.getIntegerInstance().format(fee.amount.toLong())
        return when (fee.kind) {
            "replacement" -> "Replacement fee: $amount is added to the school's card fees."
            else -> "New card fee: $amount is added to the school's card fees (covers this year)."
        }
    }

    private fun goBack() {
        if (viewModel.back()) afterBack()
    }

    // Returning to the list shows each student's card as it is now.
    private fun afterBack() {
        if (viewModel.step.value == Step.SEARCH) withToken { viewModel.search(it, binding.etSearch.text.toString()) }
    }

    private fun badge(label: String, color: Int) = TextView(requireContext()).apply {
        text = label
        textSize = 12f
        setTextColor(color)
        setPadding(dp(8), dp(3), dp(8), dp(3))
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(Color.argb(24, Color.red(color), Color.green(color), Color.blue(color)))
        }
    }

    private fun stylePill(pill: TextView, selected: Boolean) {
        val green = ContextCompat.getColor(requireContext(), R.color.main_green_color)
        pill.background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(if (selected) green else Color.WHITE)
            setStroke(dp(1), if (selected) green else Color.parseColor("#C8C8C8"))
        }
        pill.setTextColor(if (selected) Color.WHITE else DARK)
    }

    private fun styleButton(button: Button, primary: Boolean, danger: Boolean = false) {
        val green = ContextCompat.getColor(requireContext(), R.color.main_green_color)
        // The theme tints buttons; the drawable below carries the colours.
        button.backgroundTintList = null
        button.background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(if (primary) green else Color.WHITE)
            setStroke(dp(1), when { danger -> REFUSED; primary -> green; else -> Color.parseColor("#C8C8C8") })
        }
        button.setTextColor(when { danger -> REFUSED; primary -> Color.WHITE; else -> DARK })
        button.alpha = if (button.isEnabled) 1f else 0.5f
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(requireContext()).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }

    // Shows the last digits of a chip UID, enough to tell cards apart.
    private fun tail(uid: String): String = if (uid.length > 6) "…" + uid.takeLast(6).uppercase() else uid.uppercase()

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun withToken(block: (String) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val current = token ?: try {
                appDatabase.loggedInUserDao().getLastLogin().token.also { token = it }
            } catch (e: Exception) {
                return@launch
            }
            block(current)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val DARK = Color.parseColor("#002424")
        private val MUTED = Color.parseColor("#666565")
        private val ACTIVE = Color.parseColor("#01865A")
        private val NEW = Color.parseColor("#B26A00")
        private val REFUSED = Color.parseColor("#C62828")
    }
}
