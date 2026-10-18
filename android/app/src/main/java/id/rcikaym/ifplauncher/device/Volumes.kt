package id.rcikaym.ifplauncher.device

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager

data class VolumeRow(val label: String, val mounted: Boolean)

fun removableVolumes(context: Context): List<VolumeRow> {
    val manager = context.getSystemService(StorageManager::class.java) ?: return emptyList()
    return manager.storageVolumes.mapNotNull { volume ->
        if (!volume.isRemovable) return@mapNotNull null
        val state = volume.state
        val mounted = state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
        val label = volume.getDescription(context).ifBlank { "USB" }
        VolumeRow(label, mounted)
    }
}
