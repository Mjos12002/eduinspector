package com.example.inspectorappupdate.utils

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/** Keyboard behaviour shared by every screen: Done closes it, a tap outside closes it, the bottom bar hides under it. */
object Keyboard {

    fun hide(activity: Activity) {
        val view = activity.currentFocus ?: return
        (activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    /** Closes the keyboard when the touch lands outside the focused field. */
    fun closeIfOutside(activity: Activity, event: MotionEvent) {
        val focused = activity.currentFocus as? EditText ?: return
        val bounds = Rect()
        focused.getGlobalVisibleRect(bounds)
        if (!bounds.contains(event.rawX.toInt(), event.rawY.toInt())) hide(activity)
    }

    /**
     * Every field on every screen gets a Done key (multi-line fields too) that closes the keyboard; fields that
     * already set their own action keep it, and their final action still closes the keyboard.
     */
    fun closeOnDone(activity: FragmentActivity) {
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                fields(v).forEach { field ->
                    if (field.imeOptions and EditorInfo.IME_MASK_ACTION == EditorInfo.IME_ACTION_UNSPECIFIED) {
                        field.imeOptions = field.imeOptions or EditorInfo.IME_ACTION_DONE
                    }
                    if (field.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0) {
                        // A multi-line field wraps its text but shows Done instead of a new line.
                        field.setRawInputType(field.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE.inv())
                        field.setHorizontallyScrolling(false)
                        field.maxLines = Integer.MAX_VALUE
                    }
                    field.setOnEditorActionListener { _, action, _ ->
                        if (action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_ACTION_GO
                            || action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_SEND) {
                            hide(activity)
                        }
                        false
                    }
                }
            }
        }, true)
    }

    /**
     * The bottom bar hides while the keyboard is up, so it never rides on top of it. The keyboard counts as up when
     * the window's visible area is a fifth or more shorter than the screen, which works on every Android version.
     */
    fun hideBarWhileTyping(root: View, bar: View) {
        val visible = Rect()
        root.viewTreeObserver.addOnGlobalLayoutListener {
            root.getWindowVisibleDisplayFrame(visible)
            val screen = root.rootView.height
            val typing = screen > 0 && screen - visible.bottom > screen / 5
            val wanted = if (typing) View.GONE else View.VISIBLE
            if (bar.visibility != wanted && (typing || bar.tag != HIDDEN_BY_ROLE)) bar.visibility = wanted
        }
    }

    /** Set as the bar's tag when the role has a single screen, so it stays hidden after typing. */
    const val HIDDEN_BY_ROLE = "hidden-by-role"

    private fun fields(view: View): List<EditText> = when (view) {
        is EditText -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { fields(view.getChildAt(it)) }
        else -> emptyList()
    }
}
