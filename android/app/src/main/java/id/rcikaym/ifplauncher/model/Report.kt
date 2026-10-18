package id.rcikaym.ifplauncher.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdk: Int,
    val buildId: String,
    val ramTotalBytes: Long,
    val ramAvailBytes: Long,
    val storageTotalBytes: Long,
    val storageFreeBytes: Long,
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val abis: List<String>,
    val wifi: Boolean,
    val ethernet: Boolean,
    val bluetooth: Boolean,
    val interfaces: List<String>,
    val serial: String?,
)

data class ReportStep(
    val index: Int,
    val id: String,
    val title: String,
    val status: Verdict,
    val note: String,
    val atEpochMs: Long,
)

data class Report(
    val generatedAtEpochMs: Long,
    val device: DeviceSnapshot,
    val steps: List<ReportStep>,
    val expectedSteps: Int,
)

data class ReportLabels(
    val title: String,
    val generated: String,
    val deviceHeading: String,
    val resultsHeading: String,
    val summaryHeading: String,
    val note: String,
    val pass: String,
    val fail: String,
    val skip: String,
    val pending: String,
    val serialUnavailable: String,
    val present: String,
    val absent: String,
    val none: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val buildId: String,
    val ram: String,
    val storage: String,
    val screen: String,
    val abi: String,
    val wifi: String,
    val ethernet: String,
    val bluetooth: String,
    val serial: String,
    val interfaces: String,
    val ramPattern: String,
    val storagePattern: String,
    val screenPattern: String,
    val androidPattern: String,
)

fun buildReport(
    session: Session,
    device: DeviceSnapshot,
    titles: Map<StepId, String>,
    generatedAt: Long,
): Report {
    val steps = session.order.mapIndexedNotNull { index, id ->
        val outcome = session.outcomes[id] ?: return@mapIndexedNotNull null
        ReportStep(
            index = index + 1,
            id = id.name,
            title = titles[id] ?: id.name,
            status = outcome.verdict,
            note = outcome.note,
            atEpochMs = outcome.atEpochMs,
        )
    }
    return Report(generatedAt, device, steps, session.order.size)
}

fun Report.summary(): Summary {
    var pass = 0
    var fail = 0
    var skip = 0
    for (step in steps) {
        when (step.status) {
            Verdict.PASS -> pass++
            Verdict.FAIL -> fail++
            Verdict.SKIP -> skip++
        }
    }
    return Summary(pass, fail, skip, (expectedSteps - steps.size).coerceAtLeast(0))
}

fun deviceRows(device: DeviceSnapshot, labels: ReportLabels): List<Pair<String, String>> {
    val version = labels.androidPattern.format(
        device.androidRelease.ifBlank { labels.none },
        device.sdk,
    )
    return listOf(
        labels.manufacturer to device.manufacturer.ifBlank { labels.none },
        labels.model to device.model.ifBlank { labels.none },
        labels.serial to (device.serial ?: labels.serialUnavailable),
        labels.androidVersion to version,
        labels.buildId to device.buildId.ifBlank { labels.none },
        labels.ram to byteLine(labels.ramPattern, device.ramTotalBytes, device.ramAvailBytes, labels.none),
        labels.storage to byteLine(labels.storagePattern, device.storageTotalBytes, device.storageFreeBytes, labels.none),
        labels.screen to labels.screenPattern.format(device.widthPx, device.heightPx, device.densityDpi),
        labels.abi to if (device.abis.isEmpty()) labels.none else device.abis.joinToString(", "),
        labels.wifi to if (device.wifi) labels.present else labels.absent,
        labels.ethernet to if (device.ethernet) labels.present else labels.absent,
        labels.bluetooth to if (device.bluetooth) labels.present else labels.absent,
        labels.interfaces to if (device.interfaces.isEmpty()) labels.none else device.interfaces.joinToString(", "),
    )
}

fun formatReportText(report: Report, labels: ReportLabels): String = buildString {
    val summary = report.summary()
    appendLine(labels.title)
    appendLine("${labels.generated}: ${formatEpoch(report.generatedAtEpochMs)}")
    appendLine()
    appendLine(labels.deviceHeading)
    for ((label, value) in deviceRows(report.device, labels)) {
        appendLine("$label: $value")
    }
    appendLine()
    appendLine(labels.resultsHeading)
    if (report.steps.isEmpty()) appendLine(labels.none)
    for (step in report.steps) {
        val status = when (step.status) {
            Verdict.PASS -> labels.pass
            Verdict.FAIL -> labels.fail
            Verdict.SKIP -> labels.skip
        }
        appendLine("${step.index}. ${step.title} — $status — ${formatEpoch(step.atEpochMs)}")
        if (step.note.isNotBlank()) appendLine("   ${labels.note}: ${step.note}")
    }
    appendLine()
    appendLine(labels.summaryHeading)
    appendLine("${summary.pass} ${labels.pass} · ${summary.fail} ${labels.fail} · ${summary.skip} ${labels.skip}")
    if (summary.pending > 0) appendLine("${summary.pending} ${labels.pending}")
}

// ponytail: hand-rolled JSON for this fixed report. Switch to a library if fields become nested user data.
fun formatReportJson(report: Report): String {
    val summary = report.summary()
    val device = report.device
    return buildString {
        appendLine("{")
        appendLine("  \"generatedAt\": ${json(formatEpoch(report.generatedAtEpochMs))},")
        appendLine("  \"summary\": {")
        appendLine("    \"pass\": ${summary.pass},")
        appendLine("    \"fail\": ${summary.fail},")
        appendLine("    \"skip\": ${summary.skip},")
        appendLine("    \"pending\": ${summary.pending},")
        appendLine("    \"total\": ${summary.total}")
        appendLine("  },")
        appendLine("  \"device\": {")
        appendLine("    \"manufacturer\": ${json(device.manufacturer)},")
        appendLine("    \"model\": ${json(device.model)},")
        appendLine("    \"android\": ${json(device.androidRelease)},")
        appendLine("    \"sdk\": ${device.sdk},")
        appendLine("    \"buildId\": ${json(device.buildId)},")
        appendLine("    \"ramTotalBytes\": ${device.ramTotalBytes},")
        appendLine("    \"ramAvailBytes\": ${device.ramAvailBytes},")
        appendLine("    \"storageTotalBytes\": ${device.storageTotalBytes},")
        appendLine("    \"storageFreeBytes\": ${device.storageFreeBytes},")
        appendLine("    \"widthPx\": ${device.widthPx},")
        appendLine("    \"heightPx\": ${device.heightPx},")
        appendLine("    \"densityDpi\": ${device.densityDpi},")
        appendLine("    \"abis\": [${device.abis.joinToString(", ") { json(it) }}],")
        appendLine("    \"wifi\": ${device.wifi},")
        appendLine("    \"ethernet\": ${device.ethernet},")
        appendLine("    \"bluetooth\": ${device.bluetooth},")
        appendLine("    \"interfaces\": [${device.interfaces.joinToString(", ") { json(it) }}],")
        appendLine("    \"serial\": ${jsonNullable(device.serial)}")
        appendLine("  },")
        appendLine("  \"steps\": [")
        report.steps.forEachIndexed { i, step ->
            val comma = if (i == report.steps.lastIndex) "" else ","
            appendLine("    {")
            appendLine("      \"index\": ${step.index},")
            appendLine("      \"id\": ${json(step.id)},")
            appendLine("      \"title\": ${json(step.title)},")
            appendLine("      \"status\": ${json(step.status.token())},")
            appendLine("      \"note\": ${json(step.note)},")
            appendLine("      \"at\": ${json(formatEpoch(step.atEpochMs))}")
            appendLine("    }$comma")
        }
        appendLine("  ]")
        appendLine("}")
    }
}

fun formatEpoch(epochMs: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(epochMs))
}

fun reportStamp(epochMs: Long): String {
    val format = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(epochMs))
}

fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes B" else String.format(Locale.US, "%.1f %s", value, units[unit])
}

fun json(value: String): String = buildString {
    append('"')
    for (ch in value) {
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
        }
    }
    append('"')
}

private fun jsonNullable(value: String?): String = if (value == null) "null" else json(value)

private fun Verdict.token(): String = when (this) {
    Verdict.PASS -> "pass"
    Verdict.FAIL -> "fail"
    Verdict.SKIP -> "skip"
}

private fun byteLine(pattern: String, total: Long, free: Long, none: String): String {
    if (total < 0) return none
    val freeText = if (free < 0) none else formatBytes(free)
    return pattern.format(formatBytes(total), freeText)
}
