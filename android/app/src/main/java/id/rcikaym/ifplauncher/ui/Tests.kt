package id.rcikaym.ifplauncher.ui

import android.Manifest
import android.app.Activity
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import id.rcikaym.ifplauncher.R
import id.rcikaym.ifplauncher.SessionViewModel
import id.rcikaym.ifplauncher.device.OpenResult
import id.rcikaym.ifplauncher.device.hasMic
import id.rcikaym.ifplauncher.device.intentsFor
import id.rcikaym.ifplauncher.device.openFirst
import id.rcikaym.ifplauncher.device.removableVolumes
import id.rcikaym.ifplauncher.model.Coverage
import id.rcikaym.ifplauncher.model.Pan
import id.rcikaym.ifplauncher.model.StepId
import id.rcikaym.ifplauncher.model.TONE_SAMPLE_RATE
import id.rcikaym.ifplauncher.model.TouchLog
import id.rcikaym.ifplauncher.model.TouchSample
import id.rcikaym.ifplauncher.model.coverageOf
import id.rcikaym.ifplauncher.model.tonePcm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val traceColors = listOf(
    Color(0xFF7CFFB2),
    Color(0xFFFFD166),
    Color(0xFF7EB6FF),
    Color(0xFFFF8FA3),
    Color(0xFFE7FF7C),
    Color(0xFFC9B6FF),
)

private val pixelColors = listOf(
    Color.White,
    Color.Black,
    Color.Red,
    Color.Green,
    Color.Blue,
    Color.Yellow,
    Color.Cyan,
    Color.Magenta,
)

@Composable
fun TestHost(id: StepId, vm: SessionViewModel) {
    val context = LocalContext.current
    when (id) {
        StepId.MULTI_TOUCH -> MultiTouchTest(onDone = { max ->
            if (vm.note.isBlank()) vm.onNote(context.getString(R.string.multitouch_result, max))
            vm.closeTest()
        })
        StepId.DEAD_PIXEL -> DeadPixelTest(onDone = vm::closeTest)
        StepId.TOUCH_GRID -> TouchGridTest(onDone = { coverage ->
            if (vm.note.isBlank() && coverage.total > 0) {
                vm.onNote(
                    context.getString(
                        R.string.grid_result,
                        coverage.edgeHit,
                        coverage.edgeTotal,
                        coverage.centerHit,
                        coverage.centerTotal,
                    ),
                )
            }
            vm.closeTest()
        })
        StepId.SPEAKER -> SpeakerTest(
            onDone = vm::closeTest,
            onProblem = { reason ->
                vm.autoSkip(reason)
                vm.closeTest()
            },
        )
        StepId.MIC -> MicTest(
            onDone = vm::closeTest,
            onProblem = { reason ->
                vm.autoSkip(reason)
                vm.closeTest()
            },
        )
        StepId.CAMERA -> CameraTest(
            onDone = vm::closeTest,
            onProblem = { reason ->
                vm.autoSkip(reason)
            },
        )
        StepId.USB -> UsbTest(
            onDone = { names ->
                if (vm.note.isBlank() && names.isNotBlank()) {
                    vm.onNote(context.getString(R.string.usb_result, names))
                }
                vm.closeTest()
            },
            onProblem = { reason ->
                vm.autoSkip(reason)
                vm.closeTest()
            },
        )
        else -> vm.closeTest()
    }
}

@Composable
private fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previous = controller.systemBarsBehavior
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                controller.systemBarsBehavior = previous
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@Composable
private fun ChromeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BigButton(text = text, onClick = onClick, container = Pine, modifier = modifier)
}

@Composable
fun MultiTouchTest(onDone: (Int) -> Unit) {
    HideSystemBars()
    var log by remember { mutableStateOf(TouchLog()) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val samples = event.changes.map { change ->
                            TouchSample(change.id.value, change.position.x, change.position.y, change.pressed)
                        }
                        log = log.applySamples(samples)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            log.trails.forEach { (id, points) ->
                val color = traceColors[(id % traceColors.size).toInt()]
                for (i in 1 until points.size) {
                    drawLine(
                        color = color,
                        start = Offset(points[i - 1].first, points[i - 1].second),
                        end = Offset(points[i].first, points[i].second),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            log.active.forEach { (id, point) ->
                drawCircle(
                    color = traceColors[(id % traceColors.size).toInt()],
                    radius = 36f,
                    center = Offset(point.first, point.second),
                )
            }
        }
        Column(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(20.dp),
        ) {
            Text(
                stringResource(R.string.multitouch_active, log.active.size),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                stringResource(R.string.multitouch_max, log.max),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(stringResource(R.string.multitouch_hint), color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuietButton(stringResource(R.string.clear_traces), { log = log.clearTrails() }, Modifier.weight(1f))
            ChromeButton(stringResource(R.string.close_test), { onDone(log.max) }, Modifier.weight(1f))
        }
    }
}

@Composable
fun DeadPixelTest(onDone: () -> Unit) {
    HideSystemBars()
    var index by remember { mutableIntStateOf(0) }
    val color = pixelColors[index]
    val ink = if (color.luminance() < 0.45f) Color.White else Ink
    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .pointerInput(index) {
                detectTapGestures { index = (index + 1) % pixelColors.size }
            },
    ) {
        Text(
            stringResource(R.string.dead_count, index + 1, pixelColors.size),
            color = ink,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.Center),
        )
        ChromeButton(
            text = stringResource(R.string.close_test),
            onClick = onDone,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth(0.28f),
        )
    }
}

@Composable
fun TouchGridTest(onDone: (Coverage) -> Unit) {
    HideSystemBars()
    val density = LocalDensity.current
    val cellPx = with(density) { 96.dp.toPx() }
    var cols by remember { mutableIntStateOf(0) }
    var rows by remember { mutableIntStateOf(0) }
    var covered by remember { mutableStateOf(BooleanArray(0)) }
    val coverage = coverageOf(cols, rows, covered)

    Box(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .onSizeChanged { size ->
                val nextCols = (size.width / cellPx).toInt().coerceIn(6, 28)
                val nextRows = (size.height / cellPx).toInt().coerceIn(4, 18)
                if (nextCols != cols || nextRows != rows) {
                    cols = nextCols
                    rows = nextRows
                    covered = BooleanArray(nextCols * nextRows)
                }
            }
            .pointerInput(cols, rows) {
                if (cols == 0 || rows == 0) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val next = covered.copyOf()
                        var changed = false
                        for (change in event.changes) {
                            if (!change.pressed) continue
                            val c = (change.position.x / size.width * cols).toInt().coerceIn(0, cols - 1)
                            val r = (change.position.y / size.height * rows).toInt().coerceIn(0, rows - 1)
                            val i = r * cols + c
                            if (!next[i]) {
                                next[i] = true
                                changed = true
                            }
                        }
                        if (changed) covered = next
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (cols == 0 || rows == 0) return@Canvas
            val cellW = size.width / cols
            val cellH = size.height / rows
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val edge = r == 0 || c == 0 || r == rows - 1 || c == cols - 1
                    val hit = covered[r * cols + c]
                    val fill = when {
                        hit && edge -> PassGreen
                        hit -> Color(0xFF8FCBB2)
                        edge -> Color(0xFFE4C27A)
                        else -> Color(0xFFD7D2C8)
                    }
                    drawRect(
                        color = fill,
                        topLeft = Offset(c * cellW + 2f, r * cellH + 2f),
                        size = androidx.compose.ui.geometry.Size(cellW - 4f, cellH - 4f),
                    )
                }
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.grid_progress, coverage.hit, coverage.total), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.grid_edge, coverage.edgeHit, coverage.edgeTotal), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.grid_center, coverage.centerHit, coverage.centerTotal), style = MaterialTheme.typography.titleLarge)
        }
        ChromeButton(
            text = stringResource(R.string.close_test),
            onClick = { onDone(coverage) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth(0.28f),
        )
    }
}

@Composable
fun SpeakerTest(onDone: () -> Unit, onProblem: (String) -> Unit) {
    HideSystemBars()
    val context = LocalContext.current
    val failed = stringResource(R.string.reason_audio_failed)
    val scope = rememberCoroutineScope()
    var playing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val audio = context.getSystemService(AudioManager::class.java)
    val volume = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

    fun play(pan: Pan) {
        if (playing) return
        playing = true
        error = null
        scope.launch(Dispatchers.Default) {
            var track: AudioTrack? = null
            try {
                val pcm = tonePcm(440.0, 1.2, pan)
                track = buildTrack(pcm)
                track.play()
                delay(1_300)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                withContext(Dispatchers.Main) { error = failed }
            } finally {
                try {
                    track?.stop()
                } catch (_: RuntimeException) {
                }
                track?.release()
                if (isActive) withContext(Dispatchers.Main) { playing = false }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.speaker_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.speaker_hint), style = MaterialTheme.typography.bodyLarge)
        if (volume == 0) {
            Text(stringResource(R.string.volume_zero), style = MaterialTheme.typography.titleMedium, color = FailRed)
        }
        if (playing) {
            Text(stringResource(R.string.speaker_playing), style = MaterialTheme.typography.titleLarge)
        }
        error?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = FailRed) }
        BigButton(stringResource(R.string.speaker_both), { play(Pan.BOTH) }, Pine, enabled = !playing)
        BigButton(stringResource(R.string.speaker_left), { play(Pan.LEFT) }, Pine, enabled = !playing)
        BigButton(stringResource(R.string.speaker_right), { play(Pan.RIGHT) }, Pine, enabled = !playing)
        if (error != null) {
            BigButton(stringResource(R.string.skip), { onProblem(failed) }, SkipBrown)
        }
        QuietButton(stringResource(R.string.close_test), onDone)
    }
}

@Composable
fun MicTest(onDone: () -> Unit, onProblem: (String) -> Unit) {
    HideSystemBars()
    val context = LocalContext.current
    val denied = stringResource(R.string.reason_mic_denied)
    val failed = stringResource(R.string.reason_mic_failed)
    val noMic = stringResource(R.string.reason_no_mic)
    val scope = rememberCoroutineScope()
    var recording by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val file = remember { File(context.cacheDir, "mic-test.m4a") }
    val mic = hasMic(context)

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            onProblem(denied)
            return@rememberLauncherForActivityResult
        }
        scope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    recording = true
                    error = null
                }
                recordClip(context, file)
                withContext(Dispatchers.Main) {
                    recording = false
                    ready = true
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                withContext(Dispatchers.Main) {
                    recording = false
                    error = failed
                }
            }
        }
    }

    fun playBack() {
        if (playing || !ready) return
        playing = true
        scope.launch(Dispatchers.IO) {
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                player.setDataSource(file.absolutePath)
                player.prepare()
                player.start()
                while (player.isPlaying) delay(80)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                withContext(Dispatchers.Main) { error = failed }
            } finally {
                try {
                    player.release()
                } catch (_: RuntimeException) {
                }
                withContext(Dispatchers.Main) { playing = false }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.mic_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.mic_hint), style = MaterialTheme.typography.bodyLarge)
        if (!mic) Text(noMic, style = MaterialTheme.typography.titleMedium, color = SkipBrown)
        if (recording) Text(stringResource(R.string.mic_recording), style = MaterialTheme.typography.titleLarge)
        if (ready) Text(stringResource(R.string.mic_ready), style = MaterialTheme.typography.titleMedium, color = PassGreen)
        error?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = FailRed) }
        BigButton(
            text = stringResource(R.string.mic_record),
            onClick = { permission.launch(Manifest.permission.RECORD_AUDIO) },
            container = Pine,
            enabled = mic && !recording && !playing,
        )
        BigButton(
            text = stringResource(R.string.mic_play),
            onClick = ::playBack,
            container = Pine,
            enabled = ready && !recording && !playing,
        )
        if (!mic || error != null) {
            BigButton(stringResource(R.string.skip), { onProblem(error ?: noMic) }, SkipBrown)
        }
        QuietButton(stringResource(R.string.close_test), onDone)
    }
}

@Composable
fun CameraTest(onDone: () -> Unit, onProblem: (String) -> Unit) {
    val context = LocalContext.current
    val missing = stringResource(R.string.no_activity)
    val blocked = stringResource(R.string.security_blocked)
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.camera_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.camera_hint), style = MaterialTheme.typography.bodyLarge)
        BigButton(stringResource(R.string.camera_open), {
            when (openFirst(context, intentsFor(StepId.CAMERA))) {
                OpenResult.Opened -> Unit
                OpenResult.NotFound -> onProblem(missing)
                OpenResult.Blocked -> onProblem(blocked)
            }
        }, Pine)
        QuietButton(stringResource(R.string.close_test), onDone)
    }
}

@Composable
fun UsbTest(onDone: (String) -> Unit, onProblem: (String) -> Unit) {
    val context = LocalContext.current
    val failed = stringResource(R.string.reason_usb_failed)
    val mountedWord = stringResource(R.string.usb_mounted)
    val unmountedWord = stringResource(R.string.usb_unmounted)
    var rows by remember { mutableStateOf(emptyList<id.rcikaym.ifplauncher.device.VolumeRow>()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        try {
            rows = removableVolumes(context)
            error = null
        } catch (_: RuntimeException) {
            error = failed
            onProblem(failed)
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.usb_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.usb_hint), style = MaterialTheme.typography.bodyLarge)
        if (error != null) {
            Text(error!!, style = MaterialTheme.typography.titleMedium, color = FailRed)
        } else if (rows.isEmpty()) {
            Text(stringResource(R.string.usb_none), style = MaterialTheme.typography.titleLarge)
        } else {
            rows.forEach { row ->
                val state = if (row.mounted) mountedWord else unmountedWord
                Text("${row.label} · $state", style = MaterialTheme.typography.titleLarge)
            }
        }
        BigButton(stringResource(R.string.usb_refresh), { reload() }, Pine)
        QuietButton(stringResource(R.string.close_test), {
            onDone(rows.joinToString(", ") { it.label })
        })
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

private fun buildTrack(pcm: ShortArray): AudioTrack {
    val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(TONE_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build(),
        )
        .setBufferSizeInBytes(pcm.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()
    if (track.state != AudioTrack.STATE_INITIALIZED) {
        track.release()
        error("AudioTrack")
    }
    val wrote = track.write(pcm, 0, pcm.size)
    if (wrote <= 0) {
        track.release()
        error("write")
    }
    return track
}

private suspend fun recordClip(context: android.content.Context, file: File) {
    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }
    var started = false
    try {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setOutputFile(file.absolutePath)
        recorder.prepare()
        recorder.start()
        started = true
        delay(3_000)
        recorder.stop()
        started = false
    } finally {
        if (started) {
            try {
                recorder.stop()
            } catch (_: RuntimeException) {
            }
        }
        try {
            recorder.release()
        } catch (_: RuntimeException) {
        }
    }
}
