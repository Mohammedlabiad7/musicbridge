package app.toil.musicbridge.service.mirror

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveSessionSelectionTest {
    private fun playing(key: String, packageName: String = key) = SessionCandidate(key, isPlaying = true, packageName = packageName)
    private fun paused(key: String) = SessionCandidate(key, isPlaying = false, packageName = key)

    @Test
    fun keepsPlayingCurrentEvenIfAnotherJustStarted() {
        val candidates = listOf(playing("b"), playing("a"))
        assertEquals("a", selectActive(candidates, current = "a", justStarted = "b"))
    }

    @Test
    fun switchesFromPausedCurrentToJustStarted() {
        val candidates = listOf(paused("a"), playing("b"))
        assertEquals("b", selectActive(candidates, current = "a", justStarted = "b"))
    }

    @Test
    fun ignoresCallerThatIsNotPlaying() {
        val candidates = listOf(paused("a"), paused("b"))
        assertEquals("a", selectActive(candidates, current = "a", justStarted = null))
    }

    @Test
    fun nonPlayingJustStartedDoesNotTakeOverPausedCurrent() {
        val candidates = listOf(paused("b"), paused("a"))
        assertEquals("a", selectActive(candidates, current = "a", justStarted = "b"))
    }

    @Test
    fun picksPlayingSessionWhenActiveWasRemoved() {
        val candidates = listOf(paused("b"), playing("c"))
        assertEquals("c", selectActive(candidates, current = "a", justStarted = null))
    }

    @Test
    fun keepsPausedCurrentWhenNobodyPlays() {
        val candidates = listOf(paused("b"), paused("a"))
        assertEquals("a", selectActive(candidates, current = "a", justStarted = null))
    }

    @Test
    fun picksNothingWhenNothingIsTracked() {
        assertNull(selectActive(emptyList<SessionCandidate<String>>(), current = null, justStarted = null))
    }

    @Test
    fun picksNothingWhenOnlyCurrentWasRemovedAndNobodyPlays() {
        val candidates = listOf(paused("b"))
        assertNull(selectActive(candidates, current = "a", justStarted = null))
    }

    @Test
    fun picksFirstPlayingInPriorityOrder() {
        val candidates = listOf(paused("a"), playing("b"), playing("c"))
        assertEquals("b", selectActive(candidates, current = null, justStarted = null))
    }

    @Test
    fun justStartedBeatsPriorityOrderWhenCurrentIsNotPlaying() {
        val candidates = listOf(playing("b"), playing("c"), paused("a"))
        assertEquals("c", selectActive(candidates, current = "a", justStarted = "c"))
    }

    @Test
    fun switchesFromPausedCurrentToFirstPlayingWithoutJustStarted() {
        val candidates = listOf(paused("a"), playing("b"), playing("c"))
        assertEquals("b", selectActive(candidates, current = "a", justStarted = null))
    }

    @Test
    fun picksNothingOnStartupWhenAllCandidatesArePaused() {
        val candidates = listOf(paused("a"), paused("b"))
        assertNull(selectActive(candidates, current = null, justStarted = null))
    }

    @Test
    fun ignoresJustStartedThatIsNoLongerTracked() {
        val candidates = listOf(playing("b"))
        assertEquals("b", selectActive(candidates, current = null, justStarted = "gone"))
    }

    @Test
    fun skipsExcludedPlayerOnStartup() {
        val candidates = listOf(playing("a"), playing("b"))
        assertEquals("b", selectActive(candidates, null, null, setOf("a")))
    }

    @Test
    fun switchesAwayFromExcludedCurrentPlayer() {
        val candidates = listOf(playing("a"), playing("b"))
        assertEquals("b", selectActive(candidates, "a", null, setOf("a")))
    }

    @Test
    fun excludedJustStartedCannotTakeOverPausedPlayer() {
        val candidates = listOf(playing("b"), paused("a"))
        assertEquals("a", selectActive(candidates, "a", "b", setOf("b")))
    }

    @Test
    fun deactivatesWhenCurrentIsExcludedAndOtherPlayersArePaused() {
        val candidates = listOf(playing("a"), paused("b"))
        assertNull(selectActive(candidates, "a", null, setOf("a")))
    }

    @Test
    fun excludedPausedPlayerIsNotRetained() {
        assertNull(selectActive(listOf(paused("a")), "a", null, setOf("a")))
    }

    @Test
    fun excludesEverySessionOfTheSamePackage() {
        val candidates = listOf(playing("a1", "a"), playing("a2", "a"), playing("b"))
        assertEquals("b", selectActive(candidates, "a1", "a2", setOf("a")))
    }

    @Test
    fun restoredPlayingPlayerIsEligibleAgain() {
        val candidates = listOf(playing("a"))
        assertNull(selectActive(candidates, null, null, setOf("a")))
        assertEquals("a", selectActive(candidates, null, null, emptySet()))
    }

    @Test
    fun exclusionOfAnotherPlayerDoesNotInterruptCurrent() {
        val candidates = listOf(playing("a"), playing("b"))
        assertEquals("a", selectActive(candidates, "a", "b", setOf("b")))
    }
}
