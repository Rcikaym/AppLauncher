package id.rcikaym.ifplauncher.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.rcikaym.ifplauncher.R
import id.rcikaym.ifplauncher.model.ReportLabels
import id.rcikaym.ifplauncher.model.StepId

@Composable
fun rememberReportLabels(): ReportLabels = ReportLabels(
    title = stringResource(R.string.report_title),
    generated = stringResource(R.string.report_generated),
    deviceHeading = stringResource(R.string.report_device),
    resultsHeading = stringResource(R.string.report_steps),
    summaryHeading = stringResource(R.string.report_summary_heading),
    note = stringResource(R.string.report_note),
    pass = stringResource(R.string.status_pass),
    fail = stringResource(R.string.status_fail),
    skip = stringResource(R.string.status_skip),
    pending = stringResource(R.string.pending),
    serialUnavailable = stringResource(R.string.serial_unavailable),
    present = stringResource(R.string.present),
    absent = stringResource(R.string.absent),
    none = stringResource(R.string.none),
    manufacturer = stringResource(R.string.field_manufacturer),
    model = stringResource(R.string.field_model),
    androidVersion = stringResource(R.string.field_android),
    buildId = stringResource(R.string.field_build),
    ram = stringResource(R.string.field_ram),
    storage = stringResource(R.string.field_storage),
    screen = stringResource(R.string.field_screen),
    abi = stringResource(R.string.field_abi),
    wifi = stringResource(R.string.field_wifi),
    ethernet = stringResource(R.string.field_ethernet),
    bluetooth = stringResource(R.string.field_bluetooth),
    serial = stringResource(R.string.field_serial),
    interfaces = stringResource(R.string.field_interfaces),
    ramPattern = stringResource(R.string.ram_value),
    storagePattern = stringResource(R.string.storage_value),
    screenPattern = stringResource(R.string.screen_value),
    androidPattern = stringResource(R.string.android_value),
)

@StringRes
fun StepId.titleRes(): Int = when (this) {
    StepId.ABOUT -> R.string.about_title
    StepId.WIFI -> R.string.wifi_title
    StepId.BLUETOOTH -> R.string.bt_title
    StepId.ETHERNET -> R.string.eth_title
    StepId.DISPLAY -> R.string.display_title
    StepId.SOUND -> R.string.sound_title
    StepId.STORAGE -> R.string.storage_title
    StepId.DATETIME -> R.string.datetime_title
    StepId.MULTI_TOUCH -> R.string.multitouch_title
    StepId.DEAD_PIXEL -> R.string.dead_title
    StepId.TOUCH_GRID -> R.string.grid_title
    StepId.SPEAKER -> R.string.speaker_title
    StepId.MIC -> R.string.mic_title
    StepId.CAMERA -> R.string.camera_title
    StepId.USB -> R.string.usb_title
}

@StringRes
fun StepId.bodyRes(): Int = when (this) {
    StepId.ABOUT -> R.string.about_body
    StepId.WIFI -> R.string.wifi_body
    StepId.BLUETOOTH -> R.string.bt_body
    StepId.ETHERNET -> R.string.eth_body
    StepId.DISPLAY -> R.string.display_body
    StepId.SOUND -> R.string.sound_body
    StepId.STORAGE -> R.string.storage_body
    StepId.DATETIME -> R.string.datetime_body
    StepId.MULTI_TOUCH -> R.string.multitouch_body
    StepId.DEAD_PIXEL -> R.string.dead_body
    StepId.TOUCH_GRID -> R.string.grid_body
    StepId.SPEAKER -> R.string.speaker_body
    StepId.MIC -> R.string.mic_body
    StepId.CAMERA -> R.string.camera_body
    StepId.USB -> R.string.usb_body
}
