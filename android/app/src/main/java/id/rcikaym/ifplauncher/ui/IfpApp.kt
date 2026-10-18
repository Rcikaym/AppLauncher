package id.rcikaym.ifplauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import id.rcikaym.ifplauncher.Phase
import id.rcikaym.ifplauncher.SessionViewModel
import id.rcikaym.ifplauncher.device.readDevice

@Composable
fun IfpApp(vm: SessionViewModel) {
    val context = LocalContext.current
    val read = remember { readDevice(context) }
    if (vm.device == null) vm.ensureDevice(read)
    IfpTheme {
        BackHandler(enabled = vm.phase != Phase.Info) { vm.back() }
        val testing = vm.phase is Phase.Test
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(if (testing) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars)),
            color = Paper,
        ) {
            when (val phase = vm.phase) {
                Phase.Info -> DeviceInfoScreen(vm.device ?: read, onStart = vm::start)
                Phase.Steps -> ChecklistScreen(vm)
                is Phase.Test -> TestHost(phase.id, vm)
                Phase.Report -> ReportScreen(vm)
            }
        }
    }
}
