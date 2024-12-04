package io.github.feivegian.music.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import io.github.feivegian.music.BuildConfig
import io.github.feivegian.music.R
import io.github.feivegian.music.databinding.ActivityDeviceInfoBinding
import io.github.feivegian.music.utils.adjustPaddingForSystemBarInsets
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class DeviceInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceInfoBinding
    private var dumpResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data!!

            try {
                contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use { out ->
                        out.write(binding.info.text.toString().toByteArray())
                    }
                }

                Toast.makeText(this, R.string.device_info_dump_file_success, Toast.LENGTH_LONG).show()
            } catch (_: FileNotFoundException) {
                Toast.makeText(this, R.string.device_info_dump_file_fail_not_found, Toast.LENGTH_LONG).show()
            } catch (_: IOException) {
                Toast.makeText(this, R.string.device_info_dump_file_fail_io, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceInfoBinding.inflate(layoutInflater)
        binding.toolbar.adjustPaddingForSystemBarInsets(top=true)
        binding.scroller.adjustPaddingForSystemBarInsets(left=true, right=true)
        binding.buttonCenter.adjustPaddingForSystemBarInsets(left=true, right=true, bottom=true)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.device_info_title)

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.dumpInfo.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
                putExtra(Intent.EXTRA_TITLE, "device.txt")
            }

            dumpResultLauncher.launch(intent)
        }
        binding.refresh.setOnClickListener {
            Toast.makeText(this, R.string.device_info_refresh_success, Toast.LENGTH_LONG).show()
            updateInfo()
        }

        updateInfo()
    }

    private fun updateInfo() {
        val builder = StringBuilder()
        builder.appendLine(getString(R.string.device_info_device, Build.DEVICE))
        builder.appendLine(getString(R.string.device_info_brand, Build.BRAND))
        builder.appendLine(getString(R.string.device_info_model, Build.MODEL))
        builder.appendLine(getString(R.string.device_info_product, Build.PRODUCT))
        builder.appendLine(getString(R.string.device_info_manufacturer, Build.MANUFACTURER))
        builder.appendLine(getString(R.string.device_info_supported_abis, Build.SUPPORTED_ABIS.joinToString(",")))
        builder.appendLine()
        builder.appendLine(getString(R.string.device_info_os_base, Build.VERSION.BASE_OS))
        builder.appendLine(getString(R.string.device_info_os_codename, Build.VERSION.CODENAME))
        builder.appendLine(getString(R.string.device_info_os_release, Build.VERSION.RELEASE))
        builder.appendLine(getString(R.string.device_info_os_sdk_version, Build.VERSION.SDK_INT))
        builder.appendLine(getString(R.string.device_info_os_security_patch, Build.VERSION.SECURITY_PATCH))
        builder.appendLine()
        builder.appendLine(getString(R.string.device_info_package_name, BuildConfig.APPLICATION_ID))
        builder.appendLine(getString(R.string.device_info_package_version, BuildConfig.VERSION_NAME))
        builder.appendLine(getString(R.string.device_info_package_version_code, BuildConfig.VERSION_CODE))
        builder.appendLine(getString(R.string.device_info_package_type, BuildConfig.BUILD_TYPE))
        binding.info.text = builder
    }
}