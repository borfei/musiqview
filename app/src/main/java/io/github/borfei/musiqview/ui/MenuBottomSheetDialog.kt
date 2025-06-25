package io.github.borfei.musiqview.ui

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.borfei.musiqview.databinding.BottomSheetDialogMenuBinding

class MenuBottomSheetDialog : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "MenuBottomSheetDialog"
    }

    fun interface OnViewMediaInfoListener {
        fun onViewMediaInfo(dialog: MenuBottomSheetDialog)
    }
    fun interface OnOpenExternalListener {
        fun onOpenExternal(dialog: MenuBottomSheetDialog)
    }

    private var onViewMediaInfoListener: OnViewMediaInfoListener = OnViewMediaInfoListener {  }
    private var onOpenExternalListener: OnOpenExternalListener = OnOpenExternalListener {  }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val binding = BottomSheetDialogMenuBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        binding.menuViewMediaInfo.setOnClickListener {
            onViewMediaInfoListener.onViewMediaInfo(this)
        }
        binding.menuOpenExternal.setOnClickListener {
            onOpenExternalListener.onOpenExternal(this)
        }

        binding.menuMissingOption.movementMethod = LinkMovementMethod.getInstance()
        return dialog
    }

    fun setOnViewMediaInfoListener(listener: OnViewMediaInfoListener) {
        onViewMediaInfoListener = listener
    }

    fun setOnOpenExternalListener(listener: OnOpenExternalListener) {
        onOpenExternalListener = listener
    }

}