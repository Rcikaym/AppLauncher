package id.rcikaym.ifplauncher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import id.rcikaym.ifplauncher.model.DeviceSnapshot
import id.rcikaym.ifplauncher.model.Session
import id.rcikaym.ifplauncher.model.StepId
import id.rcikaym.ifplauncher.model.Verdict

sealed interface Phase {
    data object Info : Phase
    data object Steps : Phase
    data class Test(val id: StepId) : Phase
    data object Report : Phase
}

class SessionViewModel : ViewModel() {
    var session by mutableStateOf(Session())
        private set
    var phase by mutableStateOf<Phase>(Phase.Info)
        private set
    var device by mutableStateOf<DeviceSnapshot?>(null)
        private set
    var note by mutableStateOf("")
        private set
    var banner by mutableStateOf<String?>(null)
        private set
    var reportAt by mutableStateOf(0L)
        private set

    fun ensureDevice(snapshot: DeviceSnapshot) {
        if (device == null) device = snapshot
    }

    fun start() {
        phase = Phase.Steps
        syncNote()
    }

    fun onNote(value: String) {
        note = value
    }

    fun announce(text: String) {
        banner = text
    }

    fun mark(verdict: Verdict) {
        if (session.done) return
        val last = session.index == session.order.lastIndex
        session = session.mark(verdict, note, System.currentTimeMillis()).advance()
        banner = null
        if (last) openReport() else syncNote()
    }

    fun lanjut() {
        val id = session.current ?: return
        val existing = session.outcomes[id] ?: return
        val last = session.index == session.order.lastIndex
        if (note.trim() != existing.note) {
            session = session.mark(existing.verdict, note, System.currentTimeMillis())
        }
        session = session.advance()
        banner = null
        if (last) openReport() else syncNote()
    }

    fun back() {
        banner = null
        when (phase) {
            Phase.Report -> {
                phase = Phase.Steps
                if (session.done) session = session.copy(index = session.order.lastIndex)
                syncNote()
            }
            Phase.Info -> Unit
            is Phase.Test -> phase = Phase.Steps
            Phase.Steps -> {
                if (session.index == 0) {
                    phase = Phase.Info
                } else {
                    session = session.prev()
                    syncNote()
                }
            }
        }
    }

    fun openTest() {
        val id = session.current ?: return
        phase = Phase.Test(id)
    }

    fun closeTest() {
        if (phase is Phase.Test) phase = Phase.Steps
    }

    fun autoSkip(text: String) {
        val before = session
        session = session.autoSkip(text, System.currentTimeMillis())
        if (session !== before) {
            note = session.current?.let { session.outcomes[it]?.note }.orEmpty()
            banner = note
        }
    }

    fun restart() {
        session = Session()
        note = ""
        banner = null
        reportAt = 0L
        phase = Phase.Info
    }

    private fun openReport() {
        reportAt = System.currentTimeMillis()
        phase = Phase.Report
    }

    private fun syncNote() {
        note = session.current?.let { session.outcomes[it]?.note }.orEmpty()
        banner = null
    }
}
