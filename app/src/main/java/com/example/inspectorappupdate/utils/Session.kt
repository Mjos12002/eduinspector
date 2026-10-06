package com.example.inspectorappupdate.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.IndexActivity
import com.example.inspectorappupdate.SearchStudentActivity
import com.example.inspectorappupdate.entity.user.LoggedInUserEntity
import kotlinx.coroutines.launch

/** The signed-in inspector is the single row kept in the `loggedinuser` table. */
object Session {

    suspend fun current(context: Context): LoggedInUserEntity? =
        runCatching { DbUtility().dbBuilder(context.applicationContext).loggedInUserDao().findLastLogin() }
            .getOrNull()
            ?.takeIf { it.token.isNotBlank() && AppRole.canSignIn(it.role) }

    const val EXTRA_ROLE = "role"

    /** Opens the home screen as the only screen in the task, so back can't return to sign-in. */
    fun openHome(activity: Activity, role: String) {
        activity.startActivity(
            Intent(activity, SearchStudentActivity::class.java)
                .putExtra(EXTRA_ROLE, role)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        activity.finish()
    }

    /** Forgets the stored session and shows sign-in as the only screen, so back can't reopen the app. */
    fun signOut(activity: ComponentActivity, message: String? = null) {
        val appContext = activity.applicationContext
        activity.lifecycleScope.launch {
            runCatching { DbUtility().dbBuilder(appContext).loggedInUserDao().clear() }
            if (message != null) Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
            activity.startActivity(
                Intent(activity, IndexActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            activity.finish()
        }
    }
}

/** Forgets the stored session and shows sign-in as the only screen, so back can't reopen the app. */
fun Fragment.signOut() = Session.signOut(requireActivity())
