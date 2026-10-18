package id.rcikaym.ifplauncher.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.rcikaym.ifplauncher.R
import id.rcikaym.ifplauncher.SessionViewModel
import id.rcikaym.ifplauncher.export.ReportFiles
import id.rcikaym.ifplauncher.export.SaveResult
import id.rcikaym.ifplauncher.model.StepId
import id.rcikaym.ifplauncher.model.Verdict
import id.rcikaym.ifplauncher.model.buildReport
import id.rcikaym.ifplauncher.model.deviceRows
import id.rcikaym.ifplauncher.model.formatEpoch
import id.rcikaym.ifplauncher.model.formatReportJson
import id.rcikaym.ifplauncher.model.formatReportText
import id.rcikaym.ifplauncher.model.reportStamp
import id.rcikaym.ifplauncher.model.summary
import kotlinx.coroutines.launch

@Composable
fun ReportScreen(vm: SessionViewModel) {
    val device = vm.device ?: return
    val context = LocalContext.current
    val labels = rememberReportLabels()
    val titles = StepId.entries.associateWith { stringResource(it.titleRes()) }
    val report = buildReport(vm.session, device, titles, vm.reportAt)
    val text = formatReportText(report, labels)
    val json = formatReportJson(report)
    val counts = report.summary()
    val files = remember(context) { ReportFiles(context) }
    val stamp = reportStamp(report.generatedAtEpochMs)
    val textName = "ifp-laporan-$stamp.txt"
    val jsonName = "ifp-laporan-$stamp.json"
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedOk = stringResource(R.string.saved_ok)
    val saveFailed = stringResource(R.string.save_failed)
    val shareFailed = stringResource(R.string.share_failed)
    val denied = stringResource(R.string.storage_permission_denied)
    val chooser = stringResource(R.string.chooser)
    var pendingSave by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun toast(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingSave
        pendingSave = null
        if (granted) action?.invoke() else toast(denied)
    }

    fun save(name: String, mime: String, bytes: ByteArray) {
        val run = {
            when (val result = files.saveDownloads(name, mime, bytes)) {
                is SaveResult.Ok -> toast(savedOk.format(result.location))
                SaveResult.Failed -> toast(saveFailed)
            }
        }
        if (files.needsLegacyWrite() && !files.hasLegacyWrite()) {
            pendingSave = run
            permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            run()
        }
    }

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        TwoPane(
            modifier = Modifier.padding(padding),
            left = {
                Text(stringResource(R.string.report_title), style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${labels.generated}: ${formatEpoch(report.generatedAtEpochMs)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Count(counts.pass, labels.pass, PassGreen)
                    Count(counts.fail, labels.fail, FailRed)
                    Count(counts.skip, labels.skip, SkipBrown)
                }
                Text(labels.deviceHeading, style = MaterialTheme.typography.titleLarge)
                FactList(deviceRows(device, labels))
                BigButton(stringResource(R.string.share_text), {
                    if (!files.shareText(text, chooser)) toast(shareFailed)
                }, Pine)
                BigButton(stringResource(R.string.share_json), {
                    if (!files.shareFile(jsonName, "application/json", json.toByteArray(), chooser)) toast(shareFailed)
                }, Pine)
                BigButton(stringResource(R.string.save_text), {
                    save(textName, "text/plain", text.toByteArray())
                }, Ink)
                BigButton(stringResource(R.string.save_json), {
                    save(jsonName, "application/json", json.toByteArray())
                }, Ink)
                QuietButton(stringResource(R.string.back), vm::back)
                QuietButton(stringResource(R.string.restart), vm::restart)
            },
            right = {
                Text(labels.resultsHeading, style = MaterialTheme.typography.titleLarge)
                report.steps.forEach { step ->
                    StepRow(
                        index = step.index,
                        title = step.title,
                        status = when (step.status) {
                            Verdict.PASS -> labels.pass
                            Verdict.FAIL -> labels.fail
                            Verdict.SKIP -> labels.skip
                        },
                        color = when (step.status) {
                            Verdict.PASS -> PassGreen
                            Verdict.FAIL -> FailRed
                            Verdict.SKIP -> SkipBrown
                        },
                        note = step.note,
                        at = formatEpoch(step.atEpochMs),
                    )
                }
            },
        )
    }
}

@Composable
private fun Count(value: Int, label: String, color: Color) {
    Column {
        Text("$value", style = MaterialTheme.typography.headlineLarge, color = color)
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StepRow(
    index: Int,
    title: String,
    status: String,
    color: Color,
    note: String,
    at: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$index", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 4.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.bodyLarge)
            Text(at, style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.6f))
        }
        Text(
            status,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .background(color, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
