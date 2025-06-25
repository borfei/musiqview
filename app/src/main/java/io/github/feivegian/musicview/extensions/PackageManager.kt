package io.github.feivegian.musicview.extensions

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
fun <T: Any> PackageManager.setActivityEnabled(context: Context, activity: Class<T>, enabled: Boolean) {
    if (!activity.isAssignableFrom(Activity::class.java)) {
        throw IllegalArgumentException("The specified activity class is invalid")
    }
    val newState = if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    setComponentEnabledSetting(
        ComponentName(context, activity),
        newState,
        PackageManager.DONT_KILL_APP)
}

/**
 * Returns true if the activity is enabled.
 *
 * @return[Boolean]
 */
fun <T: Any> PackageManager.isActivityEnabled(context: Context, activity: Class<T>): Boolean {
    val componentName = ComponentName(context, activity)
    val state = getComponentEnabledSetting(componentName)

    if (!activity.isAssignableFrom(Activity::class.java)) {
        throw IllegalArgumentException("The specified activity class is invalid")
    }
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
