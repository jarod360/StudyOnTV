package com.studyontv.tv

import android.graphics.Rect
import android.text.Spannable
import android.text.Spanned
import android.text.method.PasswordTransformationMethod
import android.text.style.UpdateLayout
import android.view.View
import android.widget.EditText

/** Masks every character except the last newly typed one, for exactly one second. */
internal class BriefPasswordTransformation(private val field: EditText) : PasswordTransformationMethod() {
    private var shownIndex = -1
    private var marker: RefreshSpan? = null
    private var hide: Runnable? = null

    private class RefreshSpan : UpdateLayout

    override fun getTransformation(source: CharSequence, view: View): CharSequence =
        object : CharSequence {
            override val length: Int get() = source.length
            override fun get(index: Int): Char =
                if (index == shownIndex) source[index] else '*'
            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
                String(CharArray(endIndex - startIndex) { get(startIndex + it) })
            override fun toString(): String = subSequence(0, length).toString()
        }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        hide?.let { field.removeCallbacks(it) }
        hide = null
        val text = s as? Spannable ?: return
        marker?.let { text.removeSpan(it) }
        marker = null
        shownIndex = if (count == 1 && before <= 1) start else -1
        if (shownIndex < 0) return

        val span = RefreshSpan()
        text.setSpan(span, start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        marker = span
        val action = Runnable {
            shownIndex = -1
            text.removeSpan(span)
            if (marker === span) marker = null
            hide = null
        }
        hide = action
        field.postDelayed(action, 1000)
    }

    override fun onFocusChanged(
        view: View, sourceText: CharSequence, focused: Boolean,
        direction: Int, previouslyFocusedRect: Rect?
    ) {
        if (focused) return
        hide?.let { field.removeCallbacks(it) }
        hide = null
        shownIndex = -1
        marker?.let { (sourceText as? Spannable)?.removeSpan(it) }
        marker = null
    }
}
