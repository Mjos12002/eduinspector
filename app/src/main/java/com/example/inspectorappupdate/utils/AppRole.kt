package com.example.inspectorappupdate.utils

import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.viewmodel.visitor.VisitorGateViewModel.Mode

/** The roles that can sign in to the app and the bottom bar each one gets. */
object AppRole {
    const val ADMIN = "admin"
    const val INSPECTOR = "inspector"
    const val GATEKEEPER = "gatekeeper"
    const val AGENT = "agent"

    /** A bottom bar tab: the screen it opens, its label and icon. */
    data class Tab(val destination: Int, val label: Int, val icon: Int)

    private val discipline = Tab(R.id.SearchStudentFragment, R.string.tab_discipline, R.drawable.outline_attribution_24)
    private val permissions = Tab(R.id.PermissionsFragment, R.string.tab_permissions, R.drawable.outline_report_24)
    private val visitors = Tab(R.id.VisitorGateFragment, R.string.tab_visitors, R.drawable.outline_badge_24)
    private val cards = Tab(R.id.CardToolsFragment, R.string.tab_cards, R.drawable.outline_cards_24)
    private val inOut = Tab(R.id.StudentGateFragment, R.string.tab_in_out, R.drawable.presence)
    private val issue = Tab(R.id.CardIssueFragment, R.string.tab_issue_cards, R.drawable.outline_add_card_24)
    private val myCards = Tab(R.id.AgentStatsFragment, R.string.tab_my_cards, R.drawable.outline_bar_chart_24)

    /** Discipline, student permissions, visitors (register, activate cards), card tools (top up, block card, change PIN). */
    private val inspectorTabs = listOf(discipline, permissions, visitors, cards)

    /** Student in/out and visitors at the gate. */
    private val gatekeeperTabs = listOf(inOut, visitors)

    /** Card agents: issue and activate students' cards, and their own numbers. */
    private val agentTabs = listOf(issue, myCards)

    fun canSignIn(role: String?) = role in setOf(ADMIN, INSPECTOR, GATEKEEPER, AGENT)

    /** The role's tabs, in bar order. */
    fun tabs(role: String?): List<Tab> = when (role) {
        GATEKEEPER -> gatekeeperTabs
        INSPECTOR -> inspectorTabs
        AGENT -> agentTabs
        else -> (inspectorTabs + gatekeeperTabs + agentTabs).distinct()
    }

    /** Gatekeepers record visitors at the gate; inspectors register visitors and attach physical cards; admins do all. The first mode opens first. */
    fun visitorModes(role: String?): List<Mode> = when (role) {
        GATEKEEPER -> listOf(Mode.GATE)
        INSPECTOR -> listOf(Mode.REGISTER, Mode.ATTACH)
        else -> listOf(Mode.GATE, Mode.REGISTER, Mode.ATTACH)
    }

    fun startDestination(role: String?): Int = when (role) {
        GATEKEEPER -> R.id.StudentGateFragment
        AGENT -> R.id.CardIssueFragment
        else -> R.id.SearchStudentFragment
    }
}
