package id.rcikaym.ifplauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.rcikaym.ifplauncher.R
import id.rcikaym.ifplauncher.SessionViewModel
import id.rcikaym.ifplauncher.device.hasAudioOutput
import id.rcikaym.ifplauncher.device.hasCamera
import id.rcikaym.ifplauncher.device.hasMic
import id.rcikaym.ifplauncher.device.intentsFor
import id.rcikaym.ifplauncher.device.openFirst
import id.rcikaym.ifplauncher.device.OpenResult
import id.rcikaym.ifplauncher.model.StepId
import id.rcikaym.ifplauncher.model.StepKind
import id.rcikaym.ifplauncher.model.Verdict
import id.rcikaym.ifplauncher.model.kind

@Composable
fun ChecklistScreen(vm: SessionViewModel) {
    val step = vm.session.current ?: return
    val context = LocalContext.current
    val noCamera = stringResource(R.string.reason_no_camera)
    val noMic = stringResource(R.string.reason_no_mic)
    val noAudio = stringResource(R.string.reason_no_audio)
    val missing = stringResource(R.string.no_activity)
    val blocked = stringResource(R.string.security_blocked)
    val opened = stringResource(R.string.settings_opened)
    val statusPass = stringResource(R.string.status_pass)
    val statusFail = stringResource(R.string.status_fail)
    val statusSkip = stringResource(R.string.status_skip)
    val already = stringResource(R.string.already)

    LaunchedEffect(step) {
        when (step) {
            StepId.CAMERA -> if (!hasCamera(context)) vm.autoSkip(noCamera)
            StepId.MIC -> if (!hasMic(context)) vm.autoSkip(noMic)
            StepId.SPEAKER -> if (!hasAudioOutput(context)) vm.autoSkip(noAudio)
            else -> Unit
        }
    }

    val outcome = vm.session.outcomes[step]
    val index = vm.session.index
    val total = vm.session.order.size

    TwoPane(
        left = {
            Text(
                stringResource(R.string.step_of, index + 1, total),
                style = MaterialTheme.typography.bodyLarge,
                color = Pine,
            )
            ProgressBar(index, total)
            Text(stringResource(step.titleRes()), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(step.bodyRes()), style = MaterialTheme.typography.bodyLarge)
            vm.banner?.let { text ->
                Text(
                    text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3D6), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (outcome != null) {
                val word = when (outcome.verdict) {
                    Verdict.PASS -> statusPass
                    Verdict.FAIL -> statusFail
                    Verdict.SKIP -> statusSkip
                }
                Text(already.format(word), style = MaterialTheme.typography.titleMedium, color = Pine)
            }
            Spacer(Modifier.height(8.dp))
            when (step.kind) {
                StepKind.SETTINGS -> BigButton(
                    text = stringResource(R.string.open_settings),
                    onClick = {
                        when (openFirst(context, intentsFor(step))) {
                            OpenResult.Opened -> vm.announce(opened)
                            OpenResult.NotFound -> vm.autoSkip(missing)
                            OpenResult.Blocked -> vm.autoSkip(blocked)
                        }
                    },
                    container = Pine,
                )
                StepKind.TEST -> BigButton(
                    text = stringResource(R.string.start_test),
                    onClick = vm::openTest,
                    container = Pine,
                )
            }
        },
        right = {
            OutlinedTextField(
                value = vm.note,
                onValueChange = vm::onNote,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.note_label)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 2,
            )
            BigButton(stringResource(R.string.pass), { vm.mark(Verdict.PASS) }, PassGreen)
            BigButton(stringResource(R.string.fail), { vm.mark(Verdict.FAIL) }, FailRed)
            BigButton(stringResource(R.string.skip), { vm.mark(Verdict.SKIP) }, SkipBrown)
            if (outcome != null) {
                BigButton(stringResource(R.string.next), vm::lanjut, Pine)
            }
            QuietButton(
                text = if (index == 0) stringResource(R.string.info) else stringResource(R.string.back),
                onClick = vm::back,
            )
        },
    )
}

@Composable
private fun ProgressBar(index: Int, total: Int) {
    val fraction = if (total == 0) 0f else index.toFloat() / total.toFloat()
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        drawRect(color = Line)
        drawRect(color = Pine, size = Size(size.width * fraction, size.height))
    }
}
