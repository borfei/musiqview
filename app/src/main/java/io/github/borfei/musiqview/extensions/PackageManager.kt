package io.github.borfei.musiqview.extensions

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Set the specified activity to be enabled.
 *
 * When disabled, the activity cannot be started from an [Intent]
 * nor from the home screen launcher. Unless it is re-enabled again
 * by an [Activity] from the same application.
 *
 * @param[enabled] Whether the activity should be enabled
 */
fun PackageManager.setActivityEnabled(context: Context, clazz: Class<*>, enabled: Boolean) {
    val newState = if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    setComponentEnabledSetting(
        ComponentName(context, clazz),
        newState,
        PackageManager.DONT_KILL_APP)
}

/**
 * Returns true if the activity is enabled.
 *
 * @return[Boolean]
 */
fun PackageManager.isActivityEnabled(context: Context, clazz: Class<*>): Boolean {
    val componentName = ComponentName(context, clazz)
    val state = getComponentEnabledSetting(componentName)

    when (state) {
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> {
            return true
        }
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> {
            return false
        }
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> {
            return false
        }
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER -> {
            return false
        }
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> {
            return true
        }
    }

    return false
}
