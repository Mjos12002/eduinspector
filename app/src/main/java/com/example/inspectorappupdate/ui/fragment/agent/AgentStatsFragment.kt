package com.example.inspectorappupdate.ui.fragment.agent

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.SearchStudentActivity.Companion.appDatabase
import com.example.inspectorappupdate.databinding.FragmentAgentStatsBinding
import com.example.inspectorappupdate.model.agent.AgentStatsData
import com.example.inspectorappupdate.model.agent.StockCard
import com.example.inspectorappupdate.repository.agent.AgentResult
import com.example.inspectorappupdate.utils.signOut
import com.example.inspectorappupdate.viewmodel.agent.AgentStatsViewModel
import com.example.inspectorappupdate.viewmodel.agent.CardIssueViewModel
import kotlinx.coroutines.launch

// The agent's cards: printed cards in hand, cards issued today, this week and in total, per school,
// and the students whose printed card the agent holds (tap one to issue it).
@RequiresApi(Build.VERSION_CODES.O)
class AgentStatsFragment : Fragment() {

    private var _binding: FragmentAgentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgentStatsViewModel by viewModels()
    private val issueViewModel: CardIssueViewModel by activityViewModels()
    private var token: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgLogout.setOnClickListener { signOut() }
        binding.tvRefresh.setOnClickListener { withToken { viewModel.load(it) } }
        viewModel.loading.observe(viewLifecycleOwner) { busy ->
            binding.pbStats.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        }
        viewModel.stats.observe(viewLifecycleOwner) { render(it) }
        viewModel.stock.observe(viewLifecycleOwner) { renderStock(it) }

        withToken { viewModel.load(it) }
    }

    private fun render(result: AgentResult<AgentStatsData>?) {
        when (result) {
            null -> return
            is AgentResult.Failure -> {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = result.message
                return
            }
            is AgentResult.Success -> binding.tvError.visibility = View.GONE
        }
        val data = result.data
        val schools = data.schools.orEmpty()
        binding.tvSchools.text = when (schools.size) {
            0 -> "No school assigned"
            1 -> schools[0].name.orEmpty()
            else -> "${schools.size} schools"
        }

        val stats = data.stats
        binding.glNumbers.removeAllViews()
        listOf(
            "In hand" to (stats?.inHand ?: 0),
            "Issued today" to (stats?.issuedToday ?: 0),
            "Last 7 days" to (stats?.issuedWeek ?: 0),
            "Issued in total" to (stats?.issued ?: 0)
        ).forEach { (label, value) -> binding.glNumbers.addView(tile(label, value)) }
        binding.tvStockNote.text = "${stats?.handed ?: 0} printed cards handed to you, ${stats?.issuedFromStock ?: 0} of them issued."

        val names = schools.associate { it.id to it.name.orEmpty() }
        binding.llSchools.removeAllViews()
        val perSchool = stats?.schools.orEmpty()
        if (perSchool.isEmpty()) binding.llSchools.addView(text("Nothing yet.", 14f, MUTED))
        perSchool.forEach { school ->
            binding.llSchools.addView(panel().apply {
                addView(text(names[school.instituteId] ?: "School ${school.instituteId}", 15f, DARK, bold = true))
                addView(text("In hand ${school.inHand} · issued ${school.issued} (today ${school.issuedToday})", 13f, MUTED))
            })
        }

        binding.llRecent.removeAllViews()
        val recent = data.recent.orEmpty()
        if (recent.isEmpty()) binding.llRecent.addView(text("No card issued yet.", 14f, MUTED))
        recent.forEach { issue ->
            binding.llRecent.addView(panel().apply {
                addView(text(issue.student.orEmpty(), 15f, DARK, bold = true))
                addView(text(listOfNotNull(issue.issuedAt?.take(16), issue.instituteName,
                    if (issue.fromMyStock) "from my stock" else null).joinToString(" · "), 13f, MUTED))
            })
        }
    }

    private fun renderStock(stock: List<StockCard>) {
        binding.llStock.removeAllViews()
        if (stock.isEmpty()) binding.llStock.addView(text("No printed card with you.", 14f, MUTED))
        stock.forEach { card ->
            binding.llStock.addView(panel().apply {
                addView(text(card.student.orEmpty(), 15f, DARK, bold = true))
                addView(text(listOfNotNull(card.regNumber, card.instituteName, "tap to issue").joinToString(" · "), 13f, MUTED))
                setOnClickListener {
                    withToken { token ->
                        issueViewModel.openStudent(token, card.studentId)
                        findNavController().navigate(R.id.CardIssueFragment)
                    }
                }
            })
        }
    }

    private fun tile(label: String, value: Int) = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = ContextCompat.getDrawable(requireContext(), R.drawable.visitor_panel_background)
        setPadding(dp(8), dp(14), dp(8), dp(14))
        layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
            width = 0
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }
        addView(text(value.toString(), 28f, ACTIVE, bold = true).apply { gravity = Gravity.CENTER })
        addView(text(label, 13f, MUTED).apply { gravity = Gravity.CENTER })
    }

    private fun panel() = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        background = ContextCompat.getDrawable(requireContext(), R.drawable.visitor_panel_background)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(8) }
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(requireContext()).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
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
    }
}
