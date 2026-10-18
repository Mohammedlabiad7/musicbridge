package app.toil.musicbridge.service.mirror

/** A tracked session as seen by [selectActive]. */
internal data class SessionCandidate<K : Any>(val key: K, val isPlaying: Boolean, val packageName: String)

/**
 * Picks the session to mirror, or null when there is nothing to mirror.
 *
 * @param candidates every tracked session in system priority order (most recent first)
 * @param current the currently mirrored session; ignored when it is no longer among [candidates]
 * @param justStarted the session that has just reported a playing state, if any
 * @param excludedPackages apps that must not be mirrored, including their paused and newly started sessions
 */
internal fun <K : Any> selectActive(
    candidates: List<SessionCandidate<K>>,
    current: K?,
    justStarted: K?,
    excludedPackages: Set<String> = emptySet(),
): K? {
    val eligible = candidates.filter { it.packageName !in excludedPackages }
    val currentCandidate = eligible.firstOrNull { it.key == current }
    if (currentCandidate?.isPlaying == true) return currentCandidate.key

    if (justStarted != null && eligible.any { it.key == justStarted && it.isPlaying }) return justStarted

    eligible.firstOrNull { it.isPlaying }?.let { return it.key }

    // Nobody is playing: keep showing the paused player so the widget retains its controls.
    return currentCandidate?.key
}
