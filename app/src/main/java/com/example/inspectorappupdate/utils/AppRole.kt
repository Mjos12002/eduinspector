package com.example.inspectorappupdate.utils

import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.viewmodel.visitor.VisitorGateViewModel.Mode

/** The roles that can sign in to the app and the bottom bar each one gets. */
object AppRole {
    const val ADMIN = "admin"
    const val INSPECTOR = "inspector"
    const val GATEKEEPER = "gatekeeper"

    /** Discipline, student permissions, visitors (register, activate cards), card tools (top up, block card, change PIN). */
    private val inspectorTiles = listOf(
        R.id.rl_displine_deduction, R.id.rl_permissions, R.id.rl_visitor_gate, R.id.rl_card_tools
    )

    /** Student in/out and visitors at the gate. */
    private val gatekeeperTiles = listOf(R.id.rl_student_attendance, R.id.rl_visitor_gate)

    fun canSignIn(role: String?) = role in setOf(ADMIN, INSPECTOR, GATEKEEPER)

    fun tiles(role: String?): List<Int> = when (role) {
        GATEKEEPER -> gatekeeperTiles
        INSPECTOR -> inspectorTiles
        else -> (inspectorTiles + gatekeeperTiles).distinct()
    }

    /** Gatekeepers record visitors at the gate; inspectors register visitors and attach physical cards; admins do all. The first mode opens first. */
    fun visitorModes(role: String?): List<Mode> = when (role) {
        GATEKEEPER -> listOf(Mode.GATE)
        INSPECTOR -> listOf(Mode.REGISTER, Mode.ATTACH)
        else -> listOf(Mode.GATE, Mode.REGISTER, Mode.ATTACH)
    }

    fun startDestination(role: String?): Int =
        if (role == GATEKEEPER) R.id.AttendanceFragment else R.id.SearchStudentFragment
}
