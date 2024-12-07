package io.github.feivegian.music.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.feivegian.music.R
import io.github.feivegian.music.databinding.ActivityPreferenceEditorBinding
import io.github.feivegian.music.databinding.ViewholderRowSettingBinding
import io.github.feivegian.music.extensions.adjustMarginsForSystemBarInsets
import io.github.feivegian.music.extensions.adjustPaddingForSystemBarInsets

class PreferenceEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferenceEditorBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        binding = ActivityPreferenceEditorBinding.inflate(layoutInflater)
        binding.toolbar.adjustPaddingForSystemBarInsets(top=true)
        binding.settingsListReloader.adjustPaddingForSystemBarInsets(left=true, right=true, bottom=true)
        binding.settingsCreateItem.adjustMarginsForSystemBarInsets(left=true, right=true, bottom=true)
        binding.settingsCreateItem.adjustPaddingForSystemBarInsets(left=true, right=true, bottom=true)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.preference_editor_title)

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.settingsListReloader.setOnRefreshListener {
            populatePreferenceList()
        }
        binding.settingsCreateItem.setOnClickListener {
            editPreference(null)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_preference_edit_alert_title)
            .setMessage(R.string.dialog_preference_edit_alert_message)
            .setPositiveButton(R.string.dialog_preference_edit_alert_positive) { _, _ -> }
            .setCancelable(false)
            .show()

        populatePreferenceList()
    }

    private fun editPreference(key: String?) {
        val view = layoutInflater.inflate(R.layout.dialog_preference_edit, null)
        val keyField = view.findViewById<EditText>(R.id.preference_key)
        val valueField = view.findViewById<EditText>(R.id.preference_value)
        val type = view.findViewById<Spinner>(R.id.preference_type)
        val typeInteger = 0; val typeString = 1; val typeBoolean = 2; val typeLong = 3; val typeFloat = 4

        keyField.apply {
            setText(key, TextView.BufferType.EDITABLE)
        }
        valueField.apply {
            setText(key?.let { readPreference(it).toString() }, TextView.BufferType.EDITABLE)
        }
        type.apply {
            val adapter = ArrayAdapter.createFromResource(this@PreferenceEditorActivity, R.array.preference_types, android.R.layout.simple_spinner_item)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter

            this.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    when (position) {
                        typeInteger -> {
                            valueField.inputType = InputType.TYPE_CLASS_NUMBER
                        }
                        typeString -> {
                            valueField.inputType = InputType.TYPE_CLASS_TEXT
                        }
                        typeBoolean -> {
                            valueField.inputType = InputType.TYPE_CLASS_TEXT
                        }
                        typeLong -> {
                            valueField.inputType = InputType.TYPE_CLASS_NUMBER
                        }
                        typeFloat -> {
                            valueField.inputType = InputType.TYPE_CLASS_TEXT
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            if (key != null) {
                when (readPreference(key)) {
                    is Int -> {
                        setSelection(adapter.getPosition("Integer"))
                    }
                    is String -> {
                        setSelection(adapter.getPosition("String"))
                    }
                    is Boolean -> {
                        setSelection(adapter.getPosition("Boolean"))
                    }
                    is Long -> {
                        setSelection(adapter.getPosition("Long"))
                    }
                    is Float -> {
                        setSelection(adapter.getPosition("Float"))
                    }
                }
            }
        }
        AlertDialog.Builder(this)
            .setTitle(if (key != null) R.string.preference_editor_edit_item_title else R.string.preference_editor_edit_item_title_alt)
            .setView(view)
            .setPositiveButton(R.string.dialog_preference_edit_positive) { dialog, _ ->
                preferences.edit {
                    if (keyField.text.isEmpty()) {
                        Toast.makeText(this@PreferenceEditorActivity, R.string.preference_editor_edit_item_key_empty, Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    if (valueField.text.isEmpty()) {
                        Toast.makeText(this@PreferenceEditorActivity, R.string.preference_editor_edit_item_value_empty, Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    if (key != keyField.text.toString()) {
                        remove(key)
                    }
                    when (type.selectedItemPosition) {
                        typeInteger -> {
                            putInt(keyField.text.toString(), valueField.text.toString().toInt())
                        }
                        typeString -> {
                            putString(keyField.text.toString(), valueField.text.toString())
                        }
                        typeBoolean -> {
                            putBoolean(keyField.text.toString(), valueField.text.toString().toBoolean())
                        }
                        typeLong -> {
                            putLong(keyField.text.toString(), valueField.text.toString().toLong())
                        }
                        typeFloat -> {
                            putFloat(keyField.text.toString(), valueField.text.toString().toFloat())
                        }
                    }

                    apply()
                    dialog.dismiss()
                }

                populatePreferenceList()
            }
            .setNegativeButton(R.string.dialog_preference_edit_negative) { dialog, _ ->
                dialog.cancel()
            }
            .setCancelable(false)
            .show()
    }

    private fun readPreference(key: String): Any? {
        val keys = preferences.all

        if (keys != null) {
            for (entry in keys) {
                if (entry.key == key) {
                    return entry.value
                }
            }
        }

        return null
    }

    private fun populatePreferenceList() {
        val data = mutableListOf<Pair<String, Any?>>()

        preferences.all.forEach { (key, value) ->
            data.add(Pair<String, Any?>(key, value))
        }

        data.sortBy { it.first }
        Log.v(TAG, "Loaded ${data.size} keys")
        binding.settingsList.layoutManager = LinearLayoutManager(this)
        binding.settingsList.adapter = SettingsListAdapter(data)
        binding.settingsListReloader.isRefreshing = false
    }

    inner class SettingsListAdapter(private val data: MutableList<Pair<String, Any?>>) : RecyclerView.Adapter<SettingsListAdapter.ViewHolder>() {
        inner class ViewHolder(private val itemBinding: ViewholderRowSettingBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            init {
                itemBinding.root.setOnClickListener {
                    editPreference(itemBinding.key.text.toString())
                }
                itemBinding.root.setOnLongClickListener {
                    val key = itemBinding.key.text.toString()

                    AlertDialog.Builder(this@PreferenceEditorActivity)
                        .setTitle(R.string.preference_editor_delete_item_title)
                        .setMessage(getString(R.string.preference_editor_delete_item_message, key))
                        .setPositiveButton(R.string.preference_editor_delete_item_positive) { _, _ ->
                            preferences.edit().remove(key).apply()
                            data.removeAt(bindingAdapterPosition)
                            notifyItemRemoved(bindingAdapterPosition)
                            notifyItemRangeRemoved(bindingAdapterPosition, data.size - bindingAdapterPosition)
                        }
                        .setNegativeButton(R.string.preference_editor_delete_item_negative) { _, _ -> }
                        .show()

                    true
                }
            }

            fun bind(key: String, value: Any?) {
                itemBinding.key.text = key
                itemBinding.value.text = value.toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ViewholderRowSettingBinding.inflate(LayoutInflater.from(this@PreferenceEditorActivity), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            holder.bind(item.first, item.second)
        }

        override fun getItemCount(): Int = data.size
    }

    companion object {
        const val TAG = "PreferenceEditorActivity"
    }
}