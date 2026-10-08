package com.example.inspectorappupdate.ui.fragment.gate

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.databinding.FragmentStudentGateBinding
import com.example.inspectorappupdate.model.gate.StudentGateResult
import com.example.inspectorappupdate.repository.gate.GateTapResult
import com.example.inspectorappupdate.utils.signOut
import com.example.inspectorappupdate.viewmodel.gate.StudentGateViewModel
import com.example.inspectorappupdate.viewmodel.gate.StudentGateViewModel.Direction

// Student In/Out at the gate. The gatekeeper picks In or Out and taps the card; the server decides
// (permission exit or return, day scholar check-in or check-out, or refused) and the result shows here.
class StudentGateFragment : Fragment() {

    private var _binding: FragmentStudentGateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentGateViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentGateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgLogout.setOnClickListener { signOut() }
        binding.btnIn.setOnClickListener { viewModel.setDirection(Direction.IN) }
        binding.btnOut.setOnClickListener { viewModel.setDirection(Direction.OUT) }

        viewModel.direction.observe(viewLifecycleOwner) { direction ->
            styleDirection(binding.btnIn, direction == Direction.IN)
            styleDirection(binding.btnOut, direction == Direction.OUT)
            render()
        }
        viewModel.result.observe(viewLifecycleOwner) { render() }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.pbGate.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun render() {
        val leaving = viewModel.direction.value == Direction.OUT
        when (val result = viewModel.result.value) {
            null -> {
                show(
                    getString(R.string.student_gate_tap_prompt),
                    getString(if (leaving) R.string.student_gate_hint_out else R.string.student_gate_hint_in),
                    NEUTRAL
                )
                showStudent(null)
            }
            is GateTapResult.Failed -> {
                show(getString(R.string.student_gate_unread), result.message, REFUSED)
                showStudent(null)
            }
            is GateTapResult.Decided -> {
                val decision = result.result
                show(statusTitle(decision), decision.message.orEmpty(), tone(decision))
                showStudent(decision)
            }
        }
    }

    private fun statusTitle(decision: StudentGateResult): String = when (decision.result) {
        "PERMISSION_EXIT" -> "MAY LEAVE"
        "PERMISSION_RETURN" -> "RETURNED"
        "PERMISSION_RETURN_LATE" -> "RETURNED LATE"
        "DAY_CHECK_OUT" -> "CHECKED OUT"
        "CHECK_IN" -> "CHECKED IN"
        "CHECK_IN_LATE" -> "CHECKED IN · LATE"
        else -> if (decision.allowed) "RECORDED" else "NOT ALLOWED"
    }

    private fun tone(decision: StudentGateResult): Int = when {
        !decision.allowed -> REFUSED
        decision.result == "PERMISSION_RETURN_LATE" || decision.result == "CHECK_IN_LATE" -> LATE
        decision.result == "PERMISSION_EXIT" || decision.result == "DAY_CHECK_OUT" -> EXIT
        else -> ENTRY
    }

    private fun showStudent(decision: StudentGateResult?) {
        val student = decision?.student
        binding.tvStudentName.visibility = if (student != null) View.VISIBLE else View.GONE
        binding.tvStudentMeta.visibility = if (student != null) View.VISIBLE else View.GONE
        binding.tvStudentName.text = student?.name.orEmpty()
        binding.tvStudentMeta.text = listOfNotNull(
            student?.regNumber,
            if (student?.residence == "day") "Day scholar" else "Boarder"
        ).joinToString(" · ")

        val permission = decision?.permission
        val details = mutableListOf<String>()
        if (permission != null) {
            details += "Permission: ${permission.type.orEmpty().replaceFirstChar { it.uppercase() }}" +
                (permission.reason?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
            details += "Window: ${clock(permission.startTime)} – ${clock(permission.endTime)}" + dayNote(permission.startTime, permission.endTime)
            permission.exitTime?.let { details += "Left at ${clock(it)}" }
            permission.entryTime?.let { details += "Back at ${clock(it)}" }
        }
        decision?.attendance?.let { attendance ->
            attendance.checkInTime?.let { details += "In at ${it.take(5)}" }
            attendance.checkOutTime?.let { details += "Out at ${it.take(5)}" }
        }
        binding.tvDetails.visibility = if (details.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvDetails.text = details.joinToString("\n")
    }

    private fun show(status: String, message: String, tone: Int) {
        binding.tvStatus.text = status
        binding.tvStatus.setTextColor(if (tone == NEUTRAL) ContextCompat.getColor(requireContext(), R.color.card_details_text_color) else tone)
        binding.tvMessage.text = message
        binding.llResult.background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (tone == NEUTRAL) Color.WHITE else Color.argb(20, Color.red(tone), Color.green(tone), Color.blue(tone)))
            setStroke(dp(if (tone == NEUTRAL) 1 else 2), if (tone == NEUTRAL) Color.parseColor("#D1D1D1") else tone)
        }
    }

    private fun styleDirection(button: Button, selected: Boolean) {
        button.setBackgroundColor(if (selected) ContextCompat.getColor(requireContext(), R.color.main_green_color) else Color.WHITE)
        button.setTextColor(if (selected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.card_details_text_color))
    }

    // Times come as "yyyy-MM-dd HH:mm:ss".
    private fun clock(value: String?): String = value?.substringAfter(' ')?.take(5).orEmpty()

    private fun dayNote(start: String?, end: String?): String {
        val startDay = start?.substringBefore(' ')
        val endDay = end?.substringBefore(' ')
        return if (startDay != null && endDay != null && startDay != endDay) " (until $endDay)" else ""
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val ENTRY = Color.parseColor("#01865A")
        private val EXIT = Color.parseColor("#014746")
        private val LATE = Color.parseColor("#B26A00")
        private val REFUSED = Color.parseColor("#C62828")
        private val NEUTRAL = Color.parseColor("#002424")
    }
}
