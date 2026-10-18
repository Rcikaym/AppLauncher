package id.rcikaym.ifplauncher.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.rcikaym.ifplauncher.R
import id.rcikaym.ifplauncher.model.DeviceSnapshot
import id.rcikaym.ifplauncher.model.deviceRows

@Composable
fun DeviceInfoScreen(device: DeviceSnapshot, onStart: () -> Unit) {
    val labels = rememberReportLabels()
    val rows = deviceRows(device, labels)
    TwoPane(
        left = {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge, color = Pine)
            Text(stringResource(R.string.info_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.info_subtitle), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            FactList(rows)
        },
        right = {
            Spacer(Modifier.height(12.dp))
            BigButton(
                text = stringResource(R.string.start_check),
                onClick = onStart,
                container = Pine,
            )
        },
    )
}
