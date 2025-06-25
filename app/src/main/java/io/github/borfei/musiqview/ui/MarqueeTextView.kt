package io.github.borfei.musiqview.ui

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.github.borfei.musiqview.R

class MarqueeTextView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {
    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.MarqueeTextView, 0, 0).apply {
            try {
                // TODO: Implement custom attributes here
            } finally {
                recycle()
            }
        }

        // Some tweaks in order to have the marquee effect
        ellipsize = TextUtils.TruncateAt.MARQUEE
        isSingleLine = true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(true, direction, previouslyFocusedRect)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(true)
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return true
    }

    override fun isFocused(): Boolean {
        return true
    }
}