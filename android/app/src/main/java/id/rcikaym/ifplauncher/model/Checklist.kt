package id.rcikaym.ifplauncher.model

const val NOTE_LIMIT = 500

enum class Verdict { PASS, FAIL, SKIP }

enum class StepKind { SETTINGS, TEST }

enum class StepId {
    ABOUT,
    WIFI,
    BLUETOOTH,
    ETHERNET,
    DISPLAY,
    SOUND,
    STORAGE,
    DATETIME,
    MULTI_TOUCH,
    DEAD_PIXEL,
    TOUCH_GRID,
    SPEAKER,
    MIC,
    CAMERA,
    USB,
}

val StepId.kind: StepKind
    get() = when (this) {
        StepId.ABOUT,
        StepId.WIFI,
        StepId.BLUETOOTH,
        StepId.ETHERNET,
        StepId.DISPLAY,
        StepId.SOUND,
        StepId.STORAGE,
        StepId.DATETIME,
        -> StepKind.SETTINGS

        StepId.MULTI_TOUCH,
        StepId.DEAD_PIXEL,
        StepId.TOUCH_GRID,
        StepId.SPEAKER,
        StepId.MIC,
        StepId.CAMERA,
        StepId.USB,
        -> StepKind.TEST
    }

private val qcOrder = listOf(
    StepId.ABOUT,
    StepId.WIFI,
    StepId.BLUETOOTH,
    StepId.ETHERNET,
    StepId.DISPLAY,
    StepId.SOUND,
    StepId.STORAGE,
    StepId.DATETIME,
    StepId.MULTI_TOUCH,
    StepId.DEAD_PIXEL,
    StepId.TOUCH_GRID,
    StepId.SPEAKER,
    StepId.MIC,
    StepId.CAMERA,
    StepId.USB,
)

data class Outcome(
    val verdict: Verdict,
    val note: String,
    val atEpochMs: Long,
)

data class Summary(
    val pass: Int,
    val fail: Int,
    val skip: Int,
    val pending: Int,
) {
    val total: Int get() = pass + fail + skip + pending
}

data class Session(
    val outcomes: Map<StepId, Outcome> = emptyMap(),
    val index: Int = 0,
) {
    val order: List<StepId> get() = qcOrder
    val current: StepId? get() = order.getOrNull(index)
    val done: Boolean get() = index >= order.size

    fun mark(verdict: Verdict, note: String, now: Long): Session {
        val id = current ?: return this
        return copy(outcomes = outcomes + (id to Outcome(verdict, cleanNote(note), now)))
    }

    fun autoSkip(note: String, now: Long): Session {
        val id = current ?: return this
        if (outcomes.containsKey(id)) return this
        return mark(Verdict.SKIP, note, now)
    }

    fun advance(): Session = if (done) this else copy(index = index + 1)

    fun prev(): Session = if (index == 0) this else copy(index = index - 1)

    fun counts(): Summary {
        var pass = 0
        var fail = 0
        var skip = 0
        for (id in order) {
            when (outcomes[id]?.verdict) {
                Verdict.PASS -> pass++
                Verdict.FAIL -> fail++
                Verdict.SKIP -> skip++
                null -> Unit
            }
        }
        return Summary(pass, fail, skip, order.size - pass - fail - skip)
    }
}

fun cleanNote(note: String): String = note.trim().take(NOTE_LIMIT)

data class Coverage(
    val edgeHit: Int,
    val edgeTotal: Int,
    val centerHit: Int,
    val centerTotal: Int,
) {
    val hit: Int get() = edgeHit + centerHit
    val total: Int get() = edgeTotal + centerTotal
}

fun coverageOf(cols: Int, rows: Int, covered: BooleanArray): Coverage {
    if (cols <= 0 || rows <= 0 || covered.size < cols * rows) return Coverage(0, 0, 0, 0)
    var edgeHit = 0
    var edgeTotal = 0
    var centerHit = 0
    var centerTotal = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val hit = covered[r * cols + c]
            val edge = r == 0 || c == 0 || r == rows - 1 || c == cols - 1
            if (edge) {
                edgeTotal++
                if (hit) edgeHit++
            } else {
                centerTotal++
                if (hit) centerHit++
            }
        }
    }
    return Coverage(edgeHit, edgeTotal, centerHit, centerTotal)
}
