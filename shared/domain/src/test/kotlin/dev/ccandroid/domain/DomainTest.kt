package dev.ccandroid.domain

import dev.ccandroid.core.ErrorCode
import dev.ccandroid.domain.policy.HardBlockPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DomainTest {

    @Test
    fun `project creation retains fields`() {
        val project = Project(
            id = "proj_01J8Y",
            name = "Test Project",
            kind = ProjectKind.LOCAL,
            path = "/tmp/test"
        )
        assertEquals("proj_01J8Y", project.id)
        assertEquals("Test Project", project.name)
        assertEquals(ProjectKind.LOCAL, project.kind)
        assertEquals("/tmp/test", project.path)
    }

    @Test
    fun `hard block policy refuses rm commands`() {
        val refusal = HardBlockPolicy.checkCommand("rm -rf /")
        assertNotNull(refusal)
        assertEquals(ErrorCode.POLICY_BLOCK_DELETE, refusal?.code)
    }

    @Test
    fun `hard block policy refuses git push main`() {
        val refusal = HardBlockPolicy.checkCommand("git push origin main")
        assertNotNull(refusal)
        assertEquals(ErrorCode.POLICY_BLOCK_DEFAULT_BRANCH, refusal?.code)
    }

    @Test
    fun `hard block policy allows safe git commands`() {
        val refusal = HardBlockPolicy.checkCommand("git status")
        assertNull(refusal)
    }
}
