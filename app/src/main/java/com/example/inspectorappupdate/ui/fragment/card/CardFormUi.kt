package com.example.inspectorappupdate.ui.fragment.card

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.inspectorappupdate.R

/** Shared look of the Cards tab forms: choice pills, buttons, result line and the PIN keypad. */
object CardFormUi {

    val DARK = Color.parseColor("#002424")
    val MUTED = Color.parseColor("#666565")
    val REFUSED = Color.parseColor("#C62828")
    private val BORDER = Color.parseColor("#C8C8C8")

    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()

    /** One row of pills; [onPick] gets the index tapped. Pills wrap two per row when there are more than three. */
    fun pills(container: LinearLayout, labels: List<String>, selected: Int?, onPick: (Int) -> Unit) {
        val context = container.context
        container.removeAllViews()
        container.orientation = LinearLayout.VERTICAL
        val perRow = if (labels.size > 3) 2 else labels.size.coerceAtLeast(1)
        labels.chunked(perRow).forEachIndexed { rowIndex, row ->
            val line = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { if (rowIndex > 0) topMargin = dp(context, 8) }
            }
            row.forEachIndexed { i, label ->
                val index = rowIndex * perRow + i
                line.addView(TextView(context).apply {
                    text = label
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { if (i < row.size - 1) marginEnd = dp(context, 8) }
                    stylePill(this, index == selected)
                    setOnClickListener { onPick(index) }
                })
            }
            // Keeps the last row's pills the same width as the rows above.
            repeat(perRow - row.size) {
                line.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f).apply { marginStart = dp(context, 8) } })
            }
            container.addView(line)
        }
    }

    private fun stylePill(pill: TextView, selected: Boolean) {
        val context = pill.context
        val green = ContextCompat.getColor(context, R.color.main_green_color)
        pill.background = GradientDrawable().apply {
            cornerRadius = dp(context, 20).toFloat()
            setColor(if (selected) green else Color.WHITE)
            setStroke(dp(context, 1), if (selected) green else BORDER)
        }
        pill.setTextColor(if (selected) Color.WHITE else DARK)
    }

    fun styleButton(button: Button, enabled: Boolean, danger: Boolean = false) {
        val context = button.context
        val green = ContextCompat.getColor(context, R.color.main_green_color)
        button.isEnabled = enabled
        // The theme tints buttons; the drawable below carries the colours.
        button.backgroundTintList = null
        button.background = GradientDrawable().apply {
            cornerRadius = dp(context, 8).toFloat()
            setColor(if (danger) Color.WHITE else green)
            setStroke(dp(context, 1), if (danger) REFUSED else green)
        }
        button.setTextColor(if (danger) REFUSED else Color.WHITE)
        button.alpha = if (enabled) 1f else 0.5f
    }

    /** The outcome under a form's button: green when it went through, red otherwise; hidden when empty. */
    fun result(view: TextView, message: String?, ok: Boolean) {
        view.text = message
        view.visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE
        view.setTextColor(if (ok) ContextCompat.getColor(view.context, R.color.main_green_color) else REFUSED)
    }

    fun keypad(grid: GridLayout, onKey: (Char) -> Unit, onDelete: () -> Unit) {
        val context = grid.context
        grid.removeAllViews()
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫").forEach { key ->
            grid.addView(TextView(context).apply {
                text = key
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(DARK)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dp(context, 76)
                    height = dp(context, 60)
                    setMargins(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6))
                }
                if (key.isNotEmpty()) {
                    background = GradientDrawable().apply {
                        cornerRadius = dp(context, 10).toFloat()
                        setColor(Color.parseColor("#F4F4F4"))
                    }
                    contentDescription = if (key == "⌫") "Delete" else key
                    setOnClickListener { if (key == "⌫") onDelete() else onKey(key[0]) }
                }
            })
        }
    }

    fun pinDots(row: LinearLayout, length: Int, typed: Int) {
        val context = row.context
        row.removeAllViews()
        repeat(length) { index ->
            row.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 18), dp(context, 18))
                    .apply { setMargins(dp(context, 8), 0, dp(context, 8), 0) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (index < typed) DARK else Color.TRANSPARENT)
                    setStroke(dp(context, 2), DARK)
                }
            })
        }
    }
}
