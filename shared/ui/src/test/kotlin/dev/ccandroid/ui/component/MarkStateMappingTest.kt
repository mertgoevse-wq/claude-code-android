package dev.ccandroid.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [AnimatedClaudeMark] state mapping, geometry, and expression contracts
 * per `docs/03-design/logo-animation.md`.
 */
class MarkStateMappingTest {

    @Test
    fun `tool names map to correct reading mark state`() {
        val readTools = listOf("Read", "Grep", "Glob", "readFile", "grepSearch")
        readTools.forEach { tool ->
            assertEquals(
                "Tool '$tool' must map to MarkState.READING",
                MarkState.READING,
                mapEventToMarkState(runState = null, toolName = tool),
            )
        }
    }

    @Test
    fun `tool names map to correct writing mark state`() {
        val writeTools = listOf("Edit", "Write", "NotebookEdit", "editFile", "writeFile")
        writeTools.forEach { tool ->
            assertEquals(
                "Tool '$tool' must map to MarkState.WRITING",
                MarkState.WRITING,
                mapEventToMarkState(runState = null, toolName = tool),
            )
        }
    }

    @Test
    fun `bash tool maps to running mark state`() {
        val bashTools = listOf("Bash", "terminal", "executeCommand")
        bashTools.forEach { tool ->
            assertEquals(
                "Tool '$tool' must map to MarkState.RUNNING",
                MarkState.RUNNING,
                mapEventToMarkState(runState = null, toolName = tool),
            )
        }
    }

    @Test
    fun `run states map to exhaustive mark states`() {
        assertEquals(MarkState.IDLE, mapEventToMarkState(runState = "IDLE", toolName = null))
        assertEquals(MarkState.THINKING, mapEventToMarkState(runState = "THINKING", toolName = null))
        assertEquals(MarkState.READING, mapEventToMarkState(runState = "READING", toolName = null))
        assertEquals(MarkState.WRITING, mapEventToMarkState(runState = "WRITING", toolName = null))
        assertEquals(MarkState.RUNNING, mapEventToMarkState(runState = "RUNNING", toolName = null))
        assertEquals(MarkState.WAITING, mapEventToMarkState(runState = "WAITING", toolName = null))
        assertEquals(MarkState.WAITING, mapEventToMarkState(runState = "PERMISSION_REQUESTED", toolName = null))
        assertEquals(MarkState.VERIFYING, mapEventToMarkState(runState = "VERIFYING", toolName = null))
        assertEquals(MarkState.PAUSED, mapEventToMarkState(runState = "PAUSED", toolName = null))
        assertEquals(MarkState.ERROR, mapEventToMarkState(runState = "ERROR", toolName = null))
        assertEquals(MarkState.ERROR, mapEventToMarkState(runState = "FAILED", toolName = null))
        assertEquals(MarkState.DONE, mapEventToMarkState(runState = "DONE", toolName = null))
        assertEquals(MarkState.DONE, mapEventToMarkState(runState = "VERIFIED", toolName = null))
        assertEquals(MarkState.UNVERIFIED, mapEventToMarkState(runState = "UNVERIFIED", toolName = null))
    }

    @Test
    fun `idle state has no visible eyes while working states show eyes`() {
        val idleExpr = expressionFor(MarkState.IDLE)
        assertFalse("IDLE state must have no eyes visible", idleExpr.eyesVisible)

        val workingStates = MarkState.values().filter { it != MarkState.IDLE }
        assertEquals(10, workingStates.size)
        workingStates.forEach { state ->
            val expr = expressionFor(state)
            assertTrue("State '$state' must have eyes visible", expr.eyesVisible)
        }
    }

    @Test
    fun `running state has concentration asymmetry with one eye 1px wider`() {
        val runningExpr = expressionFor(MarkState.RUNNING)
        assertTrue("RUNNING state must have left eye extra width for asymmetry", runningExpr.leftEyeExtraWidth > 0f)
        assertTrue("RUNNING state lids must be narrowed", runningExpr.lidClosureLeft > 0f)
    }

    @Test
    fun `done and unverified states have distinct facial expressions`() {
        val doneExpr = expressionFor(MarkState.DONE)
        val unverifiedExpr = expressionFor(MarkState.UNVERIFIED)

        // DONE has scale pulse (> 1.0) and wide open eyes (negative lid closure)
        assertTrue("DONE state must have satisfied eye scale pulse", doneExpr.eyeScale > 1.0f)
        assertTrue("DONE state must have relaxed/wide lids", doneExpr.lidClosureLeft < 0f)

        // UNVERIFIED has 50% half-closed lids and flat scale
        assertEquals("UNVERIFIED state must have 50% lid closure", 0.50f, unverifiedExpr.lidClosureLeft, 0.001f)
        assertEquals("UNVERIFIED state must have flat 1.0 scale", 1.0f, unverifiedExpr.eyeScale, 0.001f)
    }

    @Test
    fun `claude mark geometry respects ratios from spec`() {
        assertEquals(0.30f, ClaudeMarkGeometry.INNER_RADIUS_RATIO, 0.001f)
        assertEquals(0.34f, ClaudeMarkGeometry.VALLEY_RADIUS_RATIO, 0.001f)
        assertEquals(0.46f, ClaudeMarkGeometry.OUTER_RADIUS_RATIO, 0.001f)
        assertEquals(0.11f, ClaudeMarkGeometry.EYE_DIAMETER_RATIO, 0.001f)
        assertEquals(0.24f, ClaudeMarkGeometry.EYE_SEPARATION_RATIO, 0.001f)
        assertEquals(0.40f, ClaudeMarkGeometry.PUPIL_RATIO, 0.001f)
        assertEquals(4, ClaudeMarkGeometry.ROTATION_ANGLES.size)
        assertEquals(listOf(0f, 45f, 90f, 135f), ClaudeMarkGeometry.ROTATION_ANGLES)
    }

    @Test
    fun `claude mark teardrop lobes build non-empty path`() {
        val lobe = ClaudeMarkGeometry.createTeardropLobe(100f)
        assertNotNull("Teardrop lobe path must not be null", lobe)

        val paths = ClaudeMarkGeometry.createTeardropPaths(100f)
        assertEquals(4, paths.size)
    }
}
