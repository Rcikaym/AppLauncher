package id.rcikaym.ifplauncher.model

data class TouchSample(
    val id: Long,
    val x: Float,
    val y: Float,
    val down: Boolean,
)

data class TouchLog(
    val max: Int = 0,
    val active: Map<Long, Pair<Float, Float>> = emptyMap(),
    val trails: Map<Long, List<Pair<Float, Float>>> = emptyMap(),
) {
    fun applySamples(samples: List<TouchSample>): TouchLog {
        val nextActive = active.toMutableMap()
        val nextTrails = trails.toMutableMap()
        for (sample in samples) {
            if (sample.down) {
                val point = sample.x to sample.y
                nextActive[sample.id] = point
                nextTrails[sample.id] = (nextTrails[sample.id].orEmpty() + point).takeLast(TRAIL_LIMIT)
            } else {
                nextActive.remove(sample.id)
            }
        }
        return copy(max = maxOf(max, nextActive.size), active = nextActive, trails = nextTrails)
    }

    fun clearTrails(): TouchLog = copy(active = emptyMap(), trails = emptyMap())

    private companion object {
        const val TRAIL_LIMIT = 80
    }
}
