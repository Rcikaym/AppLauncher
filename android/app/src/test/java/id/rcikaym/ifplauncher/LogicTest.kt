package id.rcikaym.ifplauncher

import id.rcikaym.ifplauncher.model.Coverage
import id.rcikaym.ifplauncher.model.DeviceSnapshot
import id.rcikaym.ifplauncher.model.NOTE_LIMIT
import id.rcikaym.ifplauncher.model.Pan
import id.rcikaym.ifplauncher.model.ReportLabels
import id.rcikaym.ifplauncher.model.Session
import id.rcikaym.ifplauncher.model.StepId
import id.rcikaym.ifplauncher.model.Summary
import id.rcikaym.ifplauncher.model.TouchLog
import id.rcikaym.ifplauncher.model.TouchSample
import id.rcikaym.ifplauncher.model.Verdict
import id.rcikaym.ifplauncher.model.buildReport
import id.rcikaym.ifplauncher.model.coverageOf
import id.rcikaym.ifplauncher.model.deviceRows
import id.rcikaym.ifplauncher.model.formatBytes
import id.rcikaym.ifplauncher.model.formatEpoch
import id.rcikaym.ifplauncher.model.formatReportJson
import id.rcikaym.ifplauncher.model.formatReportText
import id.rcikaym.ifplauncher.model.json
import id.rcikaym.ifplauncher.model.tonePcm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    @Test
    fun orderIsTheQcWalk() {
        assertEquals(
            listOf(
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
            ),
            Session().order,
        )
        assertEquals(Session().order.size, Session().order.toSet().size)
    }

    @Test
    fun markTrimsCapsAndDoesNotAdvance() {
        val session = Session().mark(Verdict.FAIL, "  " + "x".repeat(NOTE_LIMIT + 40) + "  ", 5L)
        val outcome = session.outcomes.getValue(StepId.ABOUT)
        assertEquals(Verdict.FAIL, outcome.verdict)
        assertEquals(NOTE_LIMIT, outcome.note.length)
        assertEquals(5L, outcome.atEpochMs)
        assertEquals(0, session.index)
        assertFalse(session.done)
    }

    @Test
    fun autoSkipDoesNotOverwriteOrAdvance() {
        val marked = Session().mark(Verdict.PASS, "ok", 1L)
        val kept = marked.autoSkip("nope", 2L)
        assertSame(marked, kept)
        val skipped = Session().autoSkip("kamera", 3L)
        assertEquals(Verdict.SKIP, skipped.outcomes.getValue(StepId.ABOUT).verdict)
        assertEquals("kamera", skipped.outcomes.getValue(StepId.ABOUT).note)
        assertEquals(0, skipped.index)
    }

    @Test
    fun countsAndAdvanceStopAtTheEnd() {
        var session = Session()
        session = session.mark(Verdict.PASS, "", 1L).advance()
        session = session.mark(Verdict.FAIL, "x", 2L).advance()
        session = session.autoSkip("cam", 3L)
        assertEquals(Summary(1, 1, 1, session.order.size - 3), session.counts())
        assertEquals(2, session.index)
        repeat(session.order.size) { session = session.advance() }
        assertTrue(session.done)
        assertNull(session.current)
        assertSame(session, session.advance())
        assertSame(session, session.mark(Verdict.PASS, "", 9L))
    }

    @Test
    fun prevStopsAtTheFirstStep() {
        val start = Session()
        assertSame(start, start.prev())
        val later = start.advance().advance()
        assertEquals(1, later.prev().index)
    }

    @Test
    fun reportTextAndJsonIncludeDeviceStepsAndTimestamp() {
        val session = Session()
            .mark(Verdict.PASS, "ok", 10L)
            .advance()
            .autoSkip("say \"hi\"\nnext", 20L)
        val titles = StepId.entries.associateWith { it.name.lowercase() }
        val report = buildReport(session, sampleDevice(serial = null), titles, 0L)
        assertEquals(2, report.steps.size)
        assertEquals(1, report.steps[0].index)
        assertEquals("ABOUT", report.steps[0].id)
        assertEquals(Verdict.SKIP, report.steps[1].status)
        assertEquals("1970-01-01T00:00:00Z", formatEpoch(0L))

        val text = formatReportText(report, labels())
        assertTrue(text.contains("Acme"))
        assertTrue(text.contains("Not available"))
        assertTrue(text.contains("1970-01-01T00:00:00Z"))
        assertTrue(text.contains("Wi-Fi: Present"))
        assertTrue(text.contains("Bluetooth: Not present"))
        assertTrue(text.contains("say \"hi\"\nnext"))
        assertTrue(text.contains("1 Pass"))
        assertTrue(text.contains("1 Skipped"))
        assertTrue(text.contains("13 Pending"))

        val encoded = formatReportJson(report)
        assertTrue(encoded.contains("\"serial\": null"))
        assertTrue(encoded.contains("\"status\": \"skip\""))
        assertTrue(encoded.contains("\"sdk\": 30"))
        assertTrue(encoded.contains("\"note\": \"say \\\"hi\\\"\\nnext\""))
        assertJson(encoded)
        assertFalse(Regex(",\\s*[}\\]]").containsMatchIn(encoded))

        val withSerial = formatReportJson(buildReport(Session(), sampleDevice(serial = "SN-1"), titles, 0L))
        assertTrue(withSerial.contains("\"serial\": \"SN-1\""))
        assertJson(withSerial)
    }

    @Test
    fun deviceRowsHideUnknownSerialAndMissingBytes() {
        val rows = deviceRows(sampleDevice(serial = null).copy(ramTotalBytes = -1), labels())
        assertEquals("Not available", rows.first { it.first == "Serial" }.second)
        assertEquals("—", rows.first { it.first == "RAM" }.second)
        assertEquals("11 (SDK 30)", rows.first { it.first == "Android" }.second)
    }

    @Test
    fun jsonEscapesControls() {
        assertEquals("\"say \\\"hi\\\"\\n\"", json("say \"hi\"\n"))
        assertEquals("\"a\\u0007b\"", json("a\u0007b"))
        assertEquals("\"\\\\\"", json("\\"))
    }

    @Test
    fun formatBytesUsesBinaryUnits() {
        assertEquals("—", formatBytes(-1))
        assertEquals("0 B", formatBytes(0))
        assertEquals("1.0 KB", formatBytes(1024))
        assertEquals("1.0 MB", formatBytes(1024L * 1024))
        assertEquals("1.0 GB", formatBytes(1024L * 1024 * 1024))
    }

    @Test
    fun tenFingersSetsTheMaxAndTrailsAreCapped() {
        val down = (1L..10L).map { TouchSample(it, it.toFloat(), 0f, down = true) }
        val held = TouchLog().applySamples(down)
        assertEquals(10, held.max)
        assertEquals(10, held.active.size)
        val released = held.applySamples(listOf(TouchSample(1L, 0f, 0f, down = false)))
        assertEquals(9, released.active.size)
        assertEquals(10, released.max)
        var trail = TouchLog()
        for (i in 1..100) {
            trail = trail.applySamples(listOf(TouchSample(1L, i.toFloat(), 0f, down = true)))
        }
        assertEquals(80, trail.trails.getValue(1L).size)
        assertEquals(100f, trail.trails.getValue(1L).last().first)
        val cleared = trail.clearTrails()
        assertEquals(1, cleared.max)
        assertTrue(cleared.trails.isEmpty())
        assertTrue(cleared.active.isEmpty())
    }

    @Test
    fun coverageCountsEdgeSeparatelyFromCenter() {
        val onlyCenter = BooleanArray(9)
        onlyCenter[4] = true
        assertEquals(Coverage(0, 8, 1, 1), coverageOf(3, 3, onlyCenter))
        val corner = BooleanArray(9)
        corner[0] = true
        assertEquals(Coverage(1, 8, 0, 1), coverageOf(3, 3, corner))
        assertEquals(Coverage(0, 0, 0, 0), coverageOf(0, 3, BooleanArray(0)))
    }

    @Test
    fun stereoToneSilencesTheOtherChannel() {
        val right = tonePcm(440.0, 0.05, Pan.RIGHT)
        val left = tonePcm(440.0, 0.05, Pan.LEFT)
        val both = tonePcm(440.0, 0.05, Pan.BOTH)
        assertTrue(right.filterIndexed { index, _ -> index % 2 == 0 }.all { it == 0.toShort() })
        assertTrue(right.filterIndexed { index, _ -> index % 2 == 1 }.any { it != 0.toShort() })
        assertTrue(left.filterIndexed { index, _ -> index % 2 == 1 }.all { it == 0.toShort() })
        assertTrue(left.filterIndexed { index, _ -> index % 2 == 0 }.any { it != 0.toShort() })
        assertTrue(both.filterIndexed { index, _ -> index % 2 == 0 }.any { it != 0.toShort() })
        assertTrue(both.filterIndexed { index, _ -> index % 2 == 1 }.any { it != 0.toShort() })
        assertEquals(0, right.size % 2)
    }

    private fun sampleDevice(serial: String?) = DeviceSnapshot(
        manufacturer = "Acme",
        model = "Board",
        androidRelease = "11",
        sdk = 30,
        buildId = "RP1A",
        ramTotalBytes = 4L * 1024 * 1024 * 1024,
        ramAvailBytes = 2L * 1024 * 1024 * 1024,
        storageTotalBytes = 64L * 1024 * 1024 * 1024,
        storageFreeBytes = 10L * 1024 * 1024 * 1024,
        widthPx = 1920,
        heightPx = 1080,
        densityDpi = 160,
        abis = listOf("arm64-v8a"),
        wifi = true,
        ethernet = true,
        bluetooth = false,
        interfaces = listOf("eth0", "wlan0"),
        serial = serial,
    )

    private fun labels() = ReportLabels(
        title = "Report",
        generated = "Created",
        deviceHeading = "Device",
        resultsHeading = "Steps",
        summaryHeading = "Summary",
        note = "Note",
        pass = "Pass",
        fail = "Fail",
        skip = "Skipped",
        pending = "Pending",
        serialUnavailable = "Not available",
        present = "Present",
        absent = "Not present",
        none = "—",
        manufacturer = "Manufacturer",
        model = "Model",
        androidVersion = "Android",
        buildId = "Build ID",
        ram = "RAM",
        storage = "Storage",
        screen = "Screen",
        abi = "ABI",
        wifi = "Wi-Fi",
        ethernet = "Ethernet",
        bluetooth = "Bluetooth",
        serial = "Serial",
        interfaces = "Interfaces",
        ramPattern = "%1\$s installed · %2\$s free",
        storagePattern = "%1\$s total · %2\$s free",
        screenPattern = "%1\$d x %2\$d px · %3\$d dpi",
        androidPattern = "%1\$s (SDK %2\$d)",
    )

    private fun assertJson(text: String) {
        val stack = ArrayDeque<Char>()
        var inString = false
        var escape = false
        for (ch in text) {
            if (inString) {
                when {
                    escape -> escape = false
                    ch == '\\' -> escape = true
                    ch == '"' -> inString = false
                }
            } else {
                when (ch) {
                    '"' -> inString = true
                    '{', '[' -> stack.addLast(ch)
                    '}' -> assertEquals('{', stack.removeLast())
                    ']' -> assertEquals('[', stack.removeLast())
                }
            }
        }
        assertFalse(inString)
        assertTrue(stack.isEmpty())
    }
}
