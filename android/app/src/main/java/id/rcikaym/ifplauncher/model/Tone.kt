package id.rcikaym.ifplauncher.model

import kotlin.math.PI
import kotlin.math.sin

enum class Pan { BOTH, LEFT, RIGHT }

const val TONE_SAMPLE_RATE = 44100

fun tonePcm(
    frequencyHz: Double,
    seconds: Double,
    pan: Pan,
    sampleRate: Int = TONE_SAMPLE_RATE,
): ShortArray {
    val frames = (sampleRate * seconds).toInt().coerceAtLeast(1)
    val out = ShortArray(frames * 2)
    val amp = 0.35
    for (i in 0 until frames) {
        val sample = (sin(2.0 * PI * frequencyHz * i / sampleRate) * amp * Short.MAX_VALUE)
            .toInt()
            .toShort()
        out[i * 2] = if (pan == Pan.RIGHT) 0 else sample
        out[i * 2 + 1] = if (pan == Pan.LEFT) 0 else sample
    }
    return out
}
