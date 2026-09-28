package dev.ccandroid.ui.component

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [AppIcons] contract conforming strictly to `docs/03-design/illustration-set.md`
 * and `docs/03-design/anti-slop-rules.md`.
 */
class AppIconsTest {

    private val requiredNavigationIcons = listOf(
        "Chat",
        "Projects",
        "Skills",
        "Terminal",
        "Settings",
    )

    private val requiredActionIcons = listOf(
        "Send",
        "Stop",
        "Plus",
        "Close",
        "ChevronLeft",
        "ChevronRight",
        "ChevronDown",
        "ChevronUp",
        "More",
        "Search",
        "Filter",
        "Refresh",
        "ExternalLink",
        "Copy",
        "Paste",
        "Edit",
        "Download",
        "Upload",
        "Check",
        "X",
    )

    private val requiredDomainIcons = listOf(
        "File",
        "Folder",
        "FolderOpen",
        "GitBranch",
        "GitCommit",
        "GitPullRequest",
        "Shield",
        "Key",
        "Plug",
        "Cloud",
        "Device",
        "Bug",
        "Flask",
        "Package",
        "ShieldCheck",
        "ShieldX",
        "AlertTriangle",
        "Info",
        "Clock",
        "Coins",
        "Zap",
    )

    @Test
    fun `all required navigation icons exist in AppIcons`() {
        requiredNavigationIcons.forEach { name ->
            val icon = AppIcons.all[name]
            assertNotNull("Navigation icon '$name' must exist in AppIcons", icon)
        }
    }

    @Test
    fun `all required action icons exist in AppIcons`() {
        requiredActionIcons.forEach { name ->
            val icon = AppIcons.all[name]
            assertNotNull("Action icon '$name' must exist in AppIcons", icon)
        }
    }

    @Test
    fun `all required domain icons exist in AppIcons`() {
        requiredDomainIcons.forEach { name ->
            val icon = AppIcons.all[name]
            assertNotNull("Domain icon '$name' must exist in AppIcons", icon)
        }
    }

    @Test
    fun `every icon has 24x24dp dimensions and 24x24 viewport`() {
        AppIcons.all.forEach { (name, vector) ->
            assertEquals("Icon '$name' must have 24dp defaultWidth", 24.dp, vector.defaultWidth)
            assertEquals("Icon '$name' must have 24dp defaultHeight", 24.dp, vector.defaultHeight)
            assertEquals("Icon '$name' must have viewportWidth 24f", 24f, vector.viewportWidth, 0.001f)
            assertEquals("Icon '$name' must have viewportHeight 24f", 24f, vector.viewportHeight, 0.001f)
        }
    }

    @Test
    fun `every icon contains non-empty vector paths`() {
        AppIcons.all.forEach { (name, vector) ->
            val nodeCount = vector.root.iterator().asSequence().toList().size
            assertTrue("Icon '$name' must have at least one vector node/path", nodeCount > 0)
        }
    }

    @Test
    fun `hard rule anti-slop - no trash icon exists in AppIcons`() {
        // Enforce hard block 1 & illustration-set rule: nothing deletes, no trash icon anywhere
        val bannedKeywords = listOf("trash", "delete", "remove", "bin", "garbage")

        AppIcons.all.keys.forEach { name ->
            bannedKeywords.forEach { banned ->
                assertFalse(
                    "Icon '$name' must not match banned deletion keyword '$banned'",
                    name.contains(banned, ignoreCase = true),
                )
            }
        }

        val allMemberNames = AppIcons::class.java.methods.map { it.name } +
            AppIcons::class.java.declaredFields.map { it.name }
        allMemberNames.forEach { memberName ->
            bannedKeywords.forEach { banned ->
                assertFalse(
                    "AppIcons member '$memberName' must not match banned deletion keyword '$banned'",
                    memberName.contains(banned, ignoreCase = true),
                )
            }
        }
    }

    @Test
    fun `projects aliases folder icon`() {
        assertEquals(AppIcons.Folder, AppIcons.Projects)
    }
}
