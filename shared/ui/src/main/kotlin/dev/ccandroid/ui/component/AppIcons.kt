package dev.ccandroid.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Single, unified icon set conforming strictly to `docs/03-design/illustration-set.md`.
 *
 * Source: Tabler Icons (Apache 2.0).
 * Grid: 24 × 24 dp.
 * Stroke: 2 dp, round cap, round join, fill none.
 * Hard rule from `anti-slop-rules.md`: NO TRASH ICON anywhere because nothing in this app deletes.
 */
public object AppIcons {

    private fun tablerIcon(name: String, pathData: String): ImageVector {
        val nodes = PathParser().parsePathString(pathData).toNodes()
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = nodes,
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
    }

    // ========================================================================
    // Navigation
    // ========================================================================

    public val Chat: ImageVector = tablerIcon(
        "Chat",
        "M12 20l-3 -3h-2a3 3 0 0 1 -3 -3v-6a3 3 0 0 1 3 -3h10a3 3 0 0 1 3 3v6a3 3 0 0 1 -3 3h-2l-3 3",
    )

    public val Folder: ImageVector = tablerIcon(
        "Folder",
        "M5 4h4l3 3h7a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-11a2 2 0 0 1 2 -2",
    )

    public val Projects: ImageVector = Folder

    public val Skills: ImageVector = tablerIcon(
        "Skills",
        "M4 4h5.5a1.5 1.5 0 0 1 1.5 1.5a1.5 1.5 0 0 0 3 0a1.5 1.5 0 0 1 1.5 -1.5h5.5a1 1 0 0 1 1 1v5.5a1.5 1.5 0 0 1 -1.5 1.5a1.5 1.5 0 0 0 0 3a1.5 1.5 0 0 1 1.5 1.5v5.5a1 1 0 0 1 -1 1h-5.5a1.5 1.5 0 0 1 -1.5 -1.5a1.5 1.5 0 0 0 -3 0a1.5 1.5 0 0 1 -1.5 1.5h-5.5a1 1 0 0 1 -1 -1v-5.5a1.5 1.5 0 0 1 1.5 -1.5a1.5 1.5 0 0 0 0 -3a1.5 1.5 0 0 1 -1.5 -1.5v-5.5a1 1 0 0 1 1 -1",
    )

    public val Terminal: ImageVector = tablerIcon(
        "Terminal",
        "M8 9l3 3l-3 3 M13 15l3 0 M3 4m0 2a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2z",
    )

    public val Settings: ImageVector = tablerIcon(
        "Settings",
        "M10.325 4.317c.426 -1.756 2.924 -1.756 3.35 0a1.724 1.724 0 0 0 2.573 1.066c1.543 -.94 3.31 .826 2.37 2.37a1.724 1.724 0 0 0 1.065 2.572c1.756 .426 1.756 2.924 0 3.35a1.724 1.724 0 0 0 -1.066 2.573c.94 1.543 -.826 3.31 -2.37 2.37a1.724 1.724 0 0 0 -2.572 1.065c-.426 1.756 -2.924 1.756 -3.35 0a1.724 1.724 0 0 0 -2.573 -1.066c-1.543 .94 -3.31 -.826 -2.37 -2.37a1.724 1.724 0 0 0 -1.065 -2.572c-1.756 -.426 -1.756 -2.924 0 -3.35a1.724 1.724 0 0 0 1.066 -2.573c-.94 -1.543 .826 -3.31 2.37 -2.37c1 .608 2.296 .07 2.572 -1.065z M9 12a3 3 0 1 0 6 0a3 3 0 0 0 -6 0",
    )

    // ========================================================================
    // Actions
    // ========================================================================

    public val Send: ImageVector = tablerIcon(
        "Send",
        "M10 14l11 -11 M21 3l-6.5 18a.55 .55 0 0 1 -1 0l-3.5 -7l-7 -3.5a.55 .55 0 0 1 0 -1l18 -6.5",
    )

    public val Stop: ImageVector = tablerIcon(
        "Stop",
        "M4 4m0 2a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2z",
    )

    public val Plus: ImageVector = tablerIcon(
        "Plus",
        "M12 5l0 14 M5 12l14 0",
    )

    public val Close: ImageVector = tablerIcon(
        "Close",
        "M18 6l-12 12 M6 6l12 12",
    )

    public val ChevronLeft: ImageVector = tablerIcon(
        "ChevronLeft",
        "M15 6l-6 6l6 6",
    )

    public val ChevronRight: ImageVector = tablerIcon(
        "ChevronRight",
        "M9 6l6 6l-6 6",
    )

    public val ChevronDown: ImageVector = tablerIcon(
        "ChevronDown",
        "M6 9l6 6l6 -6",
    )

    public val ChevronUp: ImageVector = tablerIcon(
        "ChevronUp",
        "M6 15l6 -6l6 6",
    )

    public val More: ImageVector = tablerIcon(
        "More",
        "M5 12m-1 0a1 1 0 1 0 2 0a1 1 0 1 0 -2 0 M12 12m-1 0a1 1 0 1 0 2 0a1 1 0 1 0 -2 0 M19 12m-1 0a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
    )

    public val Search: ImageVector = tablerIcon(
        "Search",
        "M10 10m-7 0a7 7 0 1 0 14 0a7 7 0 1 0 -14 0 M21 21l-6 -6",
    )

    public val Filter: ImageVector = tablerIcon(
        "Filter",
        "M4 4h16v2.172a2 2 0 0 1 -.586 1.414l-4.828 4.828a2 2 0 0 0 -.586 1.414v4.172l-4 2v-6.172a2 2 0 0 0 -.586 -1.414l-4.828 -4.828a2 2 0 0 1 -.586 -1.414v-2.172z",
    )

    public val Refresh: ImageVector = tablerIcon(
        "Refresh",
        "M20 11a8.1 8.1 0 0 0 -15.5 -2m-.5 -4v4h4 M4 13a8.1 8.1 0 0 0 15.5 2m.5 4v-4h-4",
    )

    public val ExternalLink: ImageVector = tablerIcon(
        "ExternalLink",
        "M12 6h-6a2 2 0 0 0 -2 2v10a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-6 M11 13l9 -9 M15 4h5v5",
    )

    public val Copy: ImageVector = tablerIcon(
        "Copy",
        "M8 8m0 2a2 2 0 0 1 2 -2h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2z M16 8v-2a2 2 0 0 0 -2 -2h-8a2 2 0 0 0 -2 2v8a2 2 0 0 0 2 2h2",
    )

    public val Paste: ImageVector = tablerIcon(
        "Paste",
        "M9 5h-2a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-12a2 2 0 0 0 -2 -2h-2 M9 3m0 2a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v0a2 2 0 0 1 -2 2h-2a2 2 0 0 1 -2 -2z",
    )

    public val Edit: ImageVector = tablerIcon(
        "Edit",
        "M7 7h-1a2 2 0 0 0 -2 2v9a2 2 0 0 0 2 2h9a2 2 0 0 0 2 -2v-1 M20.385 6.585a2.1 2.1 0 0 0 -2.97 -2.97l-8.415 8.385v3h3l8.385 -8.415z",
    )

    public val Download: ImageVector = tablerIcon(
        "Download",
        "M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2 M7 11l5 5l5 -5 M12 4l0 12",
    )

    public val Upload: ImageVector = tablerIcon(
        "Upload",
        "M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2 M7 9l5 -5l5 5 M12 4l0 12",
    )

    public val Check: ImageVector = tablerIcon(
        "Check",
        "M5 12l5 5l10 -10",
    )

    public val X: ImageVector = tablerIcon(
        "X",
        "M18 6l-12 12 M6 6l12 12",
    )

    // ========================================================================
    // Domain
    // ========================================================================

    public val File: ImageVector = tablerIcon(
        "File",
        "M14 3v4a1 1 0 0 0 1 1h4 M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z",
    )

    public val FolderOpen: ImageVector = tablerIcon(
        "FolderOpen",
        "M5 4h4l3 3h7a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-11a2 2 0 0 1 2 -2 M5 10h14l-2 9h-10z",
    )

    public val GitBranch: ImageVector = tablerIcon(
        "GitBranch",
        "M7 18m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0 M7 6m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0 M17 6m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0 M7 8l0 8 M9 18h1a6 6 0 0 0 6 -6v-4",
    )

    public val GitCommit: ImageVector = tablerIcon(
        "GitCommit",
        "M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0 M12 3l0 6 M12 15l0 6",
    )

    public val GitPullRequest: ImageVector = tablerIcon(
        "GitPullRequest",
        "M6 18m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0 M6 6m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0 M18 18m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0 M6 8v8 M11 6h5a2 2 0 0 1 2 2v8",
    )

    public val Shield: ImageVector = tablerIcon(
        "Shield",
        "M12 3a12 12 0 0 0 8.5 3a12 12 0 0 1 -8.5 15a12 12 0 0 1 -8.5 -15a12 12 0 0 0 8.5 -3",
    )

    public val Key: ImageVector = tablerIcon(
        "Key",
        "M16.55 12.85a4 4 0 0 0 -5.4 -5.4l-7.15 7.15v4h4l7.55 -5.75z",
    )

    public val Plug: ImageVector = tablerIcon(
        "Plug",
        "M9.785 6l8.215 8.215l-2.054 2.054a5.81 5.81 0 0 1 -8.215 -8.215l2.054 -2.054z M4 20l3.5 -3.5 M15 4l-3.5 3.5 M20 9l-3.5 3.5",
    )

    public val Cloud: ImageVector = tablerIcon(
        "Cloud",
        "M6.657 18c-2.572 0 -4.657 -2.007 -4.657 -4.483c0 -2.475 2.085 -4.482 4.657 -4.482c.393 -1.762 1.794 -3.2 3.675 -3.773c1.88 -.572 3.956 -.196 5.488 1.003c1.532 1.199 2.298 3.109 2.022 5.035c1.608 .112 2.898 1.455 2.898 3.1c0 1.712 -1.388 3.1 -3.1 3.1h-11z",
    )

    public val Device: ImageVector = tablerIcon(
        "Device",
        "M6 5a2 2 0 0 1 2 -2h8a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2z M11 4h2 M12 17v.01",
    )

    public val Bug: ImageVector = tablerIcon(
        "Bug",
        "M9 9v-1a3 3 0 0 1 6 0v1 M8 9h8a6 6 0 0 1 1 3v3a5 5 0 0 1 -10 0v-3a6 6 0 0 1 1 -3 M3 13h4 M17 13h4 M12 20v-6 M4 19l3.35 -2 M20 19l-3.35 -2 M4 7l3.75 1.5 M20 7l-3.75 1.5",
    )

    public val Flask: ImageVector = tablerIcon(
        "Flask",
        "M9 3l6 0 M10 9l4 0 M10 3v6l-4 11a.7 .7 0 0 0 .5 1h11a.7 .7 0 0 0 .5 -1l-4 -11v-6",
    )

    public val Package: ImageVector = tablerIcon(
        "Package",
        "M12 3l8 4.5l0 9l-8 4.5l-8 -4.5l0 -9l8 -4.5 M12 12l8 -4.5 M12 12l0 9 M12 12l-8 -4.5",
    )

    public val ShieldCheck: ImageVector = tablerIcon(
        "ShieldCheck",
        "M12 3a12 12 0 0 0 8.5 3a12 12 0 0 1 -8.5 15a12 12 0 0 1 -8.5 -15a12 12 0 0 0 8.5 -3 M9 12l2 2l4 -4",
    )

    public val ShieldX: ImageVector = tablerIcon(
        "ShieldX",
        "M12 3a12 12 0 0 0 8.5 3a12 12 0 0 1 -8.5 15a12 12 0 0 1 -8.5 -15a12 12 0 0 0 8.5 -3 M10 10l4 4 M10 14l4 -4",
    )

    public val AlertTriangle: ImageVector = tablerIcon(
        "AlertTriangle",
        "M12 4l-9 15h18l-9 -15 M12 16h.01 M12 10v3",
    )

    public val Info: ImageVector = tablerIcon(
        "Info",
        "M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0 M12 9h.01 M11 12h1v4h1",
    )

    public val Clock: ImageVector = tablerIcon(
        "Clock",
        "M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0 M12 7v5l3 3",
    )

    public val Coins: ImageVector = tablerIcon(
        "Coins",
        "M9 14m-4 0a4 4 0 1 0 8 0a4 4 0 1 0 -8 0 M12 6a4 4 0 0 1 4 4v1a4 4 0 0 1 -4 4 M15 10a4 4 0 0 1 4 4v1a4 4 0 0 1 -4 4",
    )

    public val Zap: ImageVector = tablerIcon(
        "Zap",
        "M13 3l-8 11h6l-1 7l8 -11h-6l1 -7",
    )

    /**
     * Map of all registered icons for inspection and testing.
     */
    public val all: Map<String, ImageVector> = mapOf(
        "Chat" to Chat,
        "Projects" to Projects,
        "Skills" to Skills,
        "Terminal" to Terminal,
        "Settings" to Settings,
        "Send" to Send,
        "Stop" to Stop,
        "Plus" to Plus,
        "Close" to Close,
        "ChevronLeft" to ChevronLeft,
        "ChevronRight" to ChevronRight,
        "ChevronDown" to ChevronDown,
        "ChevronUp" to ChevronUp,
        "More" to More,
        "Search" to Search,
        "Filter" to Filter,
        "Refresh" to Refresh,
        "ExternalLink" to ExternalLink,
        "Copy" to Copy,
        "Paste" to Paste,
        "Edit" to Edit,
        "Download" to Download,
        "Upload" to Upload,
        "Check" to Check,
        "X" to X,
        "File" to File,
        "Folder" to Folder,
        "FolderOpen" to FolderOpen,
        "GitBranch" to GitBranch,
        "GitCommit" to GitCommit,
        "GitPullRequest" to GitPullRequest,
        "Shield" to Shield,
        "Key" to Key,
        "Plug" to Plug,
        "Cloud" to Cloud,
        "Device" to Device,
        "Bug" to Bug,
        "Flask" to Flask,
        "Package" to Package,
        "ShieldCheck" to ShieldCheck,
        "ShieldX" to ShieldX,
        "AlertTriangle" to AlertTriangle,
        "Info" to Info,
        "Clock" to Clock,
        "Coins" to Coins,
        "Zap" to Zap,
    )
}
