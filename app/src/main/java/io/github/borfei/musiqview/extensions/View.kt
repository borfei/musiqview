package io.github.borfei.musiqview.extensions

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Adjusts the view's padding according to system bar insets
 *
 * @param[left] Adjust the view's left padding
 * @param[right] Adjust the view's right padding
 * @param[top] Adjust the view's top padding
 * @param[bottom] Adjust the view's bottom padding
 */
fun View.adjustPaddingForSystemBarInsets(left: Boolean = false,
                                  right: Boolean = false,
                                  top: Boolean = false,
                                  bottom: Boolean = false) {
    val (initialLeft, initialRight, initialTop, initialBottom) =
        listOf(paddingLeft, paddingRight, paddingTop, paddingBottom)

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        view.updatePadding(
            left = initialLeft + (if (left) insets.left else 0),
            right = initialRight + (if (right) insets.right else 0),
            top = initialTop + (if (top) insets.top else 0),
            bottom = initialBottom + (if (bottom) insets.bottom else 0)
        )

        windowInsets
    }
}
