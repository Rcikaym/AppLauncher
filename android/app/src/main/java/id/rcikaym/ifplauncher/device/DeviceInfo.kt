package id.rcikaym.ifplauncher.device

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import id.rcikaym.ifplauncher.model.DeviceSnapshot
import java.net.NetworkInterface
import java.util.Collections

fun readDevice(context: Context): DeviceSnapshot {
    val (width, height, dpi) = readScreen(context)
    val (ramTotal, ramAvail) = readRam(context)
    val (storageTotal, storageFree) = readStorage()
    val names = interfaceNames()
    val pm = context.packageManager
    return DeviceSnapshot(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        androidRelease = Build.VERSION.RELEASE.orEmpty(),
        sdk = Build.VERSION.SDK_INT,
        buildId = Build.ID.orEmpty(),
        ramTotalBytes = ramTotal,
        ramAvailBytes = ramAvail,
        storageTotalBytes = storageTotal,
        storageFreeBytes = storageFree,
        widthPx = width,
        heightPx = height,
        densityDpi = dpi,
        abis = Build.SUPPORTED_ABIS?.toList().orEmpty(),
        wifi = pm.hasSystemFeature(PackageManager.FEATURE_WIFI) ||
            names.any { it.startsWith("wlan") || it.startsWith("wifi") },
        ethernet = pm.hasSystemFeature(PackageManager.FEATURE_ETHERNET) ||
            names.any { it.startsWith("eth") },
        bluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
        interfaces = names,
        serial = readSerial(),
    )
}

fun hasCamera(context: Context): Boolean {
    val pm = context.packageManager
    if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) return true
    return try {
        context.getSystemService(CameraManager::class.java)?.cameraIdList?.isNotEmpty() == true
    } catch (_: RuntimeException) {
        false
    }
}

fun hasMic(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

fun hasAudioOutput(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)

private fun readScreen(context: Context): Triple<Int, Int, Int> {
    val metrics = context.resources.displayMetrics
    var width = metrics.widthPixels
    var height = metrics.heightPixels
    var dpi = metrics.densityDpi
    try {
        val wm = context.getSystemService(WindowManager::class.java) ?: return Triple(width, height, dpi)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
            dpi = context.resources.configuration.densityDpi
        } else {
            val real = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(real)
            width = real.widthPixels
            height = real.heightPixels
            dpi = real.densityDpi
        }
    } catch (_: RuntimeException) {
        // Keep the resource metrics.
    }
    return Triple(width, height, dpi)
}

private fun readRam(context: Context): Pair<Long, Long> {
    return try {
        val am = context.getSystemService(ActivityManager::class.java) ?: return -1L to -1L
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        info.totalMem to info.availMem
    } catch (_: RuntimeException) {
        -1L to -1L
    }
}

private fun readStorage(): Pair<Long, Long> {
    return try {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.totalBytes to stat.availableBytes
    } catch (_: RuntimeException) {
        -1L to -1L
    }
}

private fun interfaceNames(): List<String> {
    return try {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .map { it.name }
            .filter { it.isNotBlank() && it != "lo" }
            .sorted()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun readSerial(): String? {
    return try {
        val value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Build.getSerial()
        } else {
            @Suppress("DEPRECATION")
            Build.SERIAL
        }
        value?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
    } catch (_: SecurityException) {
        null
    }
}
