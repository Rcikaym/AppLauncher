package id.rcikaym.ifplauncher.device

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings
import android.util.AndroidRuntimeException
import id.rcikaym.ifplauncher.model.StepId

sealed interface OpenResult {
    data object Opened : OpenResult
    data object NotFound : OpenResult
    data object Blocked : OpenResult
}

fun intentsFor(id: StepId): List<Intent> = when (id) {
    StepId.ABOUT -> listOf(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
    StepId.WIFI -> listOf(Intent(Settings.ACTION_WIFI_SETTINGS))
    StepId.BLUETOOTH -> listOf(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    StepId.ETHERNET -> listOf(
        Intent("android.settings.ETHERNET_SETTINGS"),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
    )
    StepId.DISPLAY -> listOf(Intent(Settings.ACTION_DISPLAY_SETTINGS))
    StepId.SOUND -> listOf(Intent(Settings.ACTION_SOUND_SETTINGS))
    StepId.STORAGE -> listOf(
        Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        Intent(Settings.ACTION_MEMORY_CARD_SETTINGS),
    )
    StepId.DATETIME -> listOf(Intent(Settings.ACTION_DATE_SETTINGS))
    StepId.CAMERA -> listOf(
        Intent("android.media.action.STILL_IMAGE_CAMERA"),
        Intent(MediaStore.ACTION_IMAGE_CAPTURE),
    )
    else -> emptyList()
}

fun openFirst(context: Context, intents: List<Intent>): OpenResult {
    if (intents.isEmpty()) return OpenResult.NotFound
    var blocked = false
    for (intent in intents) {
        when (openOne(context, intent)) {
            OpenResult.Opened -> return OpenResult.Opened
            OpenResult.Blocked -> blocked = true
            OpenResult.NotFound -> Unit
        }
    }
    return if (blocked) OpenResult.Blocked else OpenResult.NotFound
}

private fun openOne(context: Context, intent: Intent): OpenResult {
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        OpenResult.Opened
    } catch (_: ActivityNotFoundException) {
        OpenResult.NotFound
    } catch (_: SecurityException) {
        OpenResult.Blocked
    } catch (_: AndroidRuntimeException) {
        OpenResult.NotFound
    }
}
