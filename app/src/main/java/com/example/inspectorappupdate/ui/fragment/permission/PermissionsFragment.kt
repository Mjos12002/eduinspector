package com.example.inspectorappupdate.ui.fragment.permission

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.SearchStudentActivity.Companion.appDatabase
import com.example.inspectorappupdate.databinding.FragmentPermissionsBinding
import com.example.inspectorappupdate.model.card.CardModel
import com.example.inspectorappupdate.model.permission.CreatePermissionRequest
import com.example.inspectorappupdate.model.permission.Permission
import com.example.inspectorappupdate.model.permission.PermissionStudent
import com.example.inspectorappupdate.repository.visitor.VisitorResult
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import com.example.inspectorappupdate.viewmodel.permission.PermissionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Student permissions to leave: Issued lists those usable on a chosen day (valid, or in use while out);
// New starts from the student's card, then the type and the time window.
@RequiresApi(Build.VERSION_CODES.O)
class PermissionsFragment : Fragment() {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PermissionViewModel by viewModels()
    private val cardViewModel: CardViewModel by activityViewModels()

    private val types = listOf(
        "hospital" to "Hospital", "home" to "Home", "medical" to "Medical",
        "emergency" to "Emergency", "other" to "Other"
    )
    private val durations = listOf(1 to "1 hour", 2 to "2 hours", 4 to "4 hours", 24 to "1 day")

    private var selectedType = "hospital"
    private val start: Calendar = Calendar.getInstance()
    private val end: Calendar = Calendar.getInstance()
    private var token: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTabIssued.setOnClickListener { showTab(false) }
        binding.btnTabNew.setOnClickListener { showTab(true) }
        showTab(false)

        // Issued: day filter.
        binding.btnDay.setOnClickListener { pickDay() }
        binding.btnToday.setOnClickListener { withToken { viewModel.setDay(it, Calendar.getInstance()) } }
        viewModel.day.observe(viewLifecycleOwner) { day ->
            binding.btnDay.text = DAY_SHOWN.format(day.time)
            binding.btnToday.visibility = if (sameDay(day, Calendar.getInstance())) View.GONE else View.VISIBLE
        }

        // New permission form.
        buildPills(binding.llTypes, types.map { it.second }) { index -> selectedType = types[index].first; renderTypes() }
        buildPills(binding.llDurations, durations.map { it.second }) { index -> setDuration(durations[index].first) }
        binding.btnStartDate.setOnClickListener { pickDate(start) }
        binding.btnStartTime.setOnClickListener { pickTime(start) }
        binding.btnEndDate.setOnClickListener { pickDate(end) }
        binding.btnEndTime.setOnClickListener { pickTime(end) }
        binding.btnIssue.setOnClickListener { submit() }
        binding.btnCancel.setOnClickListener { viewModel.setStudent(null) }

        viewModel.loading.observe(viewLifecycleOwner) { busy -> showProgress(busy || viewModel.saving.value == true) }
        viewModel.saving.observe(viewLifecycleOwner) { busy ->
            showProgress(busy || viewModel.loading.value == true)
            binding.btnIssue.isEnabled = !busy
        }
        viewModel.permissions.observe(viewLifecycleOwner) { renderList(it) }
        viewModel.student.observe(viewLifecycleOwner) { renderStudent(it) }
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            val (ok, message) = result
            binding.tvFormError.visibility = if (ok) View.GONE else View.VISIBLE
            binding.tvFormError.text = if (ok) "" else message
            if (ok) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                showTab(false)
            }
            viewModel.saveResultShown()
        }

        // A card tapped while this screen is open opens the form for that student. The value held from before is ignored.
        val before = cardViewModel.cardLiveData.value
        cardViewModel.cardLiveData.observe(viewLifecycleOwner) { card -> if (card !== before) onCardRead(card) }

        withToken { viewModel.load(it) }
    }

    private fun onCardRead(card: CardModel?) {
        val data = card?.data
        if (card == null || data == null || card.error || data.reg_number.isBlank()) {
            showTab(true)
            viewModel.setStudent(null)
            Toast.makeText(requireContext(), card?.message?.ifBlank { null } ?: "This card is not a student card", Toast.LENGTH_SHORT).show()
            return
        }
        showTab(true)
        viewModel.setStudent(PermissionStudent("${data.first_name} ${data.last_name}".trim(), data.reg_number, data.card_status))
    }

    private fun renderStudent(student: PermissionStudent?) {
        binding.tvTapPrompt.visibility = if (student == null) View.VISIBLE else View.GONE
        binding.llForm.visibility = if (student == null) View.GONE else View.VISIBLE
        binding.tvFormError.visibility = View.GONE
        if (student == null) return

        val active = student.cardStatus.equals("active", ignoreCase = true)
        binding.tvStudentName.text = student.name
        binding.tvStudentMeta.text = "${student.regNumber} · card ${student.cardStatus.lowercase()}"
        binding.llFields.visibility = if (active) View.VISIBLE else View.GONE
        binding.btnIssue.visibility = if (active) View.VISIBLE else View.GONE
        if (!active) {
            binding.tvFormError.text = "Only a student with an active card can get a permission."
            binding.tvFormError.visibility = View.VISIBLE
            return
        }

        // A fresh form for each student: leaving now, back in 2 hours.
        selectedType = types.first().first
        start.timeInMillis = System.currentTimeMillis()
        start.set(Calendar.SECOND, 0)
        setDuration(2)
        binding.etReason.setText("")
        renderTypes()
    }

    private fun showTab(new: Boolean) {
        binding.llIssued.visibility = if (new) View.GONE else View.VISIBLE
        binding.llNew.visibility = if (new) View.VISIBLE else View.GONE
        styleTab(binding.btnTabIssued, !new)
        styleTab(binding.btnTabNew, new)
    }

    private fun submit() {
        val student = viewModel.student.value ?: return
        if (!end.after(start)) {
            binding.tvFormError.text = "Back by must be after the leaving time"
            binding.tvFormError.visibility = View.VISIBLE
            return
        }
        binding.tvFormError.visibility = View.GONE
        val reason = binding.etReason.text.toString().trim()
        val request = CreatePermissionRequest(
            input = student.regNumber,
            permissionType = selectedType,
            reason = reason.ifEmpty { null },
            startTime = API_TIME.format(start.time),
            endTime = API_TIME.format(end.time)
        )
        withToken { viewModel.create(it, request) }
    }

    // ------------------------------------------------------------------ issued list

    private fun renderList(result: VisitorResult<List<Permission>>?) {
        binding.llPermissionList.removeAllViews()
        binding.tvIssuedMessage.visibility = View.GONE
        when (result) {
            null -> return
            is VisitorResult.Failure -> showIssuedMessage(result.message)
            is VisitorResult.Success -> {
                if (result.data.isEmpty()) showIssuedMessage(getString(R.string.permissions_none))
                result.data.forEach { binding.llPermissionList.addView(permissionRow(it)) }
            }
        }
    }

    private fun permissionRow(permission: Permission): View {
        val type = types.firstOrNull { it.first == permission.permissionType }?.second ?: permission.permissionType.orEmpty()
        val out = permission.exitTime != null
        val overdue = out && parse(permission.endTime)?.before(Date()) == true
        val (state, tone) = when {
            overdue -> "OVERDUE · out since ${clock(permission.exitTime)}, due back ${clock(permission.endTime)}" to OVERDUE
            out -> "IN USE · out since ${clock(permission.exitTime)}" to IN_USE
            else -> "VALID · not left yet" to VALID
        }

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1), Color.parseColor("#D1D1D1"))
            }
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(8) }
        }
        row.addView(text(state, 12f, tone, bold = true))
        row.addView(text(permission.studentName.orEmpty(), 17f, ContextCompat.getColor(requireContext(), R.color.card_details_text_color), bold = true))
        row.addView(text(
            listOf(
                type,
                "${short(permission.startTime)} → ${short(permission.endTime)}",
                permission.reason?.takeIf { it.isNotBlank() }
            ).filterNotNull().joinToString("\n"),
            14f,
            ContextCompat.getColor(requireContext(), R.color.text_color_hint)
        ))
        return row
    }

    private fun showIssuedMessage(message: String) {
        binding.tvIssuedMessage.text = message
        binding.tvIssuedMessage.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------------ form helpers

    private fun buildPills(container: LinearLayout, labels: List<String>, onPick: (Int) -> Unit) {
        container.removeAllViews()
        labels.forEachIndexed { index, label ->
            val pill = TextView(requireContext()).apply {
                text = label
                textSize = 15f
                setPadding(dp(14), dp(9), dp(14), dp(9))
                setOnClickListener { onPick(index) }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginEnd = dp(8) }
            }
            stylePill(pill, false)
            container.addView(pill)
        }
    }

    private fun renderTypes() {
        types.forEachIndexed { index, type -> stylePill(binding.llTypes.getChildAt(index) as TextView, type.first == selectedType) }
    }

    private fun stylePill(pill: TextView, selected: Boolean) {
        val green = ContextCompat.getColor(requireContext(), R.color.main_green_color)
        pill.background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(if (selected) green else Color.WHITE)
            setStroke(dp(1), if (selected) green else Color.parseColor("#C8C8C8"))
        }
        pill.setTextColor(if (selected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.card_details_text_color))
    }

    private fun setDuration(hours: Int) {
        end.timeInMillis = start.timeInMillis
        end.add(Calendar.HOUR_OF_DAY, hours)
        renderTimes()
    }

    // The system date and time pickers.
    private fun pickDate(target: Calendar) {
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val length = end.timeInMillis - start.timeInMillis
            target.set(year, month, day)
            // Moving the leaving day keeps the length of the permission.
            if (target === start && length > 0) end.timeInMillis = start.timeInMillis + length
            renderTimes()
        }, target.get(Calendar.YEAR), target.get(Calendar.MONTH), target.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.timeInMillis
        }.show()
    }

    private fun pickTime(target: Calendar) {
        TimePickerDialog(requireContext(), { _, hour, minute ->
            val length = end.timeInMillis - start.timeInMillis
            target.set(Calendar.HOUR_OF_DAY, hour)
            target.set(Calendar.MINUTE, minute)
            target.set(Calendar.SECOND, 0)
            if (target === start && length > 0) end.timeInMillis = start.timeInMillis + length
            renderTimes()
        }, target.get(Calendar.HOUR_OF_DAY), target.get(Calendar.MINUTE), true).show()
    }

    private fun renderTimes() {
        binding.btnStartDate.text = DATE_SHOWN.format(start.time)
        binding.btnStartTime.text = TIME_SHOWN.format(start.time)
        binding.btnEndDate.text = DATE_SHOWN.format(end.time)
        binding.btnEndTime.text = TIME_SHOWN.format(end.time)
    }

    private fun pickDay() {
        val day = viewModel.day.value ?: Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, date ->
            val chosen = Calendar.getInstance().apply { set(year, month, date) }
            withToken { viewModel.setDay(it, chosen) }
        }, day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ------------------------------------------------------------------ small helpers

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(requireContext()).apply {
        text = value
        textSize = size
        setTextColor(color)
        setLineSpacing(0f, 1.15f)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun parse(value: String?): Date? = try {
        value?.let { API_TIME.parse(it) }
    } catch (e: Exception) {
        null
    }

    private fun short(value: String?): String = parse(value)?.let { SHORT_SHOWN.format(it) } ?: "—"

    private fun clock(value: String?): String = parse(value)?.let { TIME_SHOWN.format(it) } ?: "—"

    private fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun styleTab(button: TextView, selected: Boolean) {
        button.setBackgroundColor(if (selected) ContextCompat.getColor(requireContext(), R.color.main_green_color) else Color.WHITE)
        button.setTextColor(if (selected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.card_details_text_color))
    }

    private fun showProgress(busy: Boolean) {
        binding.pbPermissions.visibility = if (busy) View.VISIBLE else View.INVISIBLE
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
        private val API_TIME = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        private val DAY_SHOWN = SimpleDateFormat("EEE d MMM yyyy", Locale.US)
        private val DATE_SHOWN = SimpleDateFormat("EEE d MMM", Locale.US)
        private val TIME_SHOWN = SimpleDateFormat("HH:mm", Locale.US)
        private val SHORT_SHOWN = SimpleDateFormat("d MMM HH:mm", Locale.US)

        private val VALID = Color.parseColor("#01865A")
        private val IN_USE = Color.parseColor("#1565C0")
        private val OVERDUE = Color.parseColor("#C62828")
    }
}
