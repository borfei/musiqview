package io.github.spir0th.music.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding

fun View.adjustPaddingForSystemBarInsets(left: Boolean = false,
                                  top: Boolean = false,
                                  right: Boolean = false,
                                  bottom: Boolean = false) {
    val (initialLeft, initialTop, initialRight, initialBottom) =
        listOf(paddingLeft, paddingTop, paddingRight, paddingBottom)

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.updatePadding(
            left = initialLeft + (if (left) insets.left else 0),
            top = initialTop + (if (top) insets.top else 0),
            right = initialRight + (if (right) insets.right else 0),
            bottom = initialBottom + (if (bottom) insets.bottom else 0)
        )

        windowInsets
    }
}

fun View.adjustMarginsForSystemBarInsets(left: Boolean = false,
                                         top: Boolean = false,
                                         right: Boolean = false,
                                         bottom: Boolean = false) {
    val (initialLeft, initialTop, initialRight, initialBottom) =
        listOf(paddingLeft, paddingTop, paddingRight, paddingBottom)

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(
                left = initialLeft + (if (left) insets.left else 0),
                top = initialTop + (if (top) insets.top else 0),
                right = initialRight + (if (right) insets.right else 0),
                bottom = initialBottom + (if (bottom) insets.bottom else 0)
            )
        }

        windowInsets
    }
}