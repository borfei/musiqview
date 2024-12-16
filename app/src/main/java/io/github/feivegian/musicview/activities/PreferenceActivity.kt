package io.github.feivegian.musicview.activities

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import io.github.feivegian.musicview.R
import io.github.feivegian.musicview.databinding.ActivityPreferenceBinding
import io.github.feivegian.musicview.extensions.adjustPaddingForSystemBarInsets
import io.github.feivegian.musicview.extensions.restart
import io.github.feivegian.musicview.fragments.PreferenceFragment

class PreferenceActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private lateinit var binding: ActivityPreferenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding
        binding = ActivityPreferenceBinding.inflate(layoutInflater)
        binding.toolbar.adjustPaddingForSystemBarInsets(top=true)
        binding.preferenceRoot.adjustPaddingForSystemBarInsets(left=true, right=true, bottom=true)
        // Set the root view as binding's root view & the action bar as toolbar
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Tweaks for the action bar
        supportActionBar?.setTitle(R.string.preference_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.preference, PreferenceFragment())
            .commit()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fragment = pref.fragment?.let { supportFragmentManager.fragmentFactory.instantiate(classLoader, it) }
        fragment?.arguments = pref.extras

        supportFragmentManager.beginTransaction()
            .replace(R.id.preference, fragment!!)
            .addToBackStack(null)
            .commit()

        return true
    }

    fun notifyRestart() {
        Snackbar.make(binding.root, R.string.snackbar_restart_required, Snackbar.LENGTH_LONG)
            .setAction(R.string.snackbar_restart_required_action) { _ ->
                application.restart(componentName)
            }
            .show()
    }

    companion object {
        const val TAG = "SettingsActivity"
    }
}