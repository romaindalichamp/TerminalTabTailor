package com.terminaltabtailor.util

import com.terminaltabtailor.enum.TabNameTypeEnum
import com.terminaltabtailor.settings.TerminalTabTailorSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat

/**
 * The naming half of the "Follow the shell's current folder" naming style: turning whatever path the
 * shell reports into the tab name. The tracking half lives in
 * [com.terminaltabtailor.service.CurrentDirectoryTabNameTracker] and needs a running IDE, so it is
 * out of reach here — which is why the decision of *what* to call the tab sits in this util instead.
 */
class CurrentDirectoryNameTest {

    @Test
    fun `takes the last segment of a unix path`() {
        assertEquals("TerminalTabTailor", TerminalTabsUtil.directoryNameOf("/home/romain/TerminalTabTailor"))
    }

    @Test
    fun `takes the last segment of a windows path`() {
        assertEquals("TerminalTabTailor", TerminalTabsUtil.directoryNameOf("C:\\workspace\\TerminalTabTailor"))
    }

    /**
     * A WSL, Docker or Git Bash terminal on Windows reports POSIX paths, so the separator of the
     * shell is not the separator of the IDE's host OS.
     */
    @Test
    fun `takes the last segment of a unix path on a windows host`() {
        assertEquals("TerminalTabTailor", TerminalTabsUtil.directoryNameOf("/mnt/c/workspace/TerminalTabTailor"))
    }

    @Test
    fun `ignores a trailing separator`() {
        assertEquals("src", TerminalTabsUtil.directoryNameOf("/home/romain/src/"))
        assertEquals("src", TerminalTabsUtil.directoryNameOf("C:\\workspace\\src\\"))
    }

    @Test
    fun `keeps the drive letter for a windows drive root`() {
        assertEquals("C:", TerminalTabsUtil.directoryNameOf("C:\\"))
    }

    @Test
    fun `keeps the separator for the filesystem root`() {
        assertEquals("/", TerminalTabsUtil.directoryNameOf("/"))
    }

    @Test
    fun `keeps a path the shell reports unexpanded`() {
        assertEquals("~", TerminalTabsUtil.directoryNameOf("~"))
    }

    /**
     * The engines return null or an empty string until shell integration has reported a directory;
     * the tracker leaves such a tab alone rather than naming it after nothing.
     */
    @Test
    fun `has no name for a blank path`() {
        assertNull(TerminalTabsUtil.directoryNameOf(""))
        assertNull(TerminalTabsUtil.directoryNameOf("   "))
    }

    @Test
    fun `follows cmd on windows`() {
        assertTrue(TerminalTabsUtil.canFollowWorkingDirectory(listOf("cmd.exe"), isWindows = true))
        assertTrue(
            TerminalTabsUtil.canFollowWorkingDirectory(listOf("C:\\Windows\\System32\\CMD.EXE"), isWindows = true),
            "Windows spells the same shell either case, and set lookups are case sensitive"
        )
    }

    /**
     * The shells this plugin cannot follow on Windows, each for its own reason: WSL and ssh hide the
     * real shell behind a launcher, Git Bash and MSYS keep an emulated directory the Win32 process
     * never learns, and IntelliJ injects no shell integration for any of them, so nothing is left to
     * read. Their tabs keep the name they opened with rather than showing a folder long left.
     *
     * powershell and pwsh belong here too, for a different reason: `Set-Location` never calls Win32's
     * `SetCurrentDirectory()` — PowerShell/PowerShell#17149, open upstream — so the OS-level directory
     * this plugin can read never moves either, even though the shell itself did. Verified end to end:
     * every layer between this plugin and the OS query (TerminalWorkingDirectoryManager,
     * ProcessInfoUtil, WinConPtyProcess) was decompiled to rule out a cache on our side of the call —
     * there is none, the query is fresh every time, and it is PowerShell that never updates what it's
     * asked. Trying anyway would not fix anything: the tab would freeze on whatever it opened with
     * while a poll that can never succeed keeps burning a blocking, up-to-2-second OS query every tick.
     */
    @Test
    fun `does not follow a windows shell whose directory never moves`() {
        assertFalse(TerminalTabsUtil.canFollowWorkingDirectory(listOf("wsl.exe", "-d", "Ubuntu"), isWindows = true))
        assertFalse(TerminalTabsUtil.canFollowWorkingDirectory(listOf("ubuntu2204.exe"), isWindows = true))
        assertFalse(
            TerminalTabsUtil.canFollowWorkingDirectory(
                listOf("C:\\Program Files\\Git\\bin\\bash.exe"), isWindows = true
            )
        )
        assertFalse(TerminalTabsUtil.canFollowWorkingDirectory(listOf("ssh", "romain@buildbox"), isWindows = true))
        assertFalse(TerminalTabsUtil.canFollowWorkingDirectory(listOf("powershell.exe"), isWindows = true))
        assertFalse(TerminalTabsUtil.canFollowWorkingDirectory(listOf("pwsh.exe"), isWindows = true))
    }

    /** On a Unix host the shell is the process itself, so the very same names are followed. */
    @Test
    fun `follows the native shells of a unix host`() {
        assertTrue(TerminalTabsUtil.canFollowWorkingDirectory(listOf("/bin/bash"), isWindows = false))
        assertTrue(TerminalTabsUtil.canFollowWorkingDirectory(listOf("/usr/bin/zsh"), isWindows = false))
        assertTrue(TerminalTabsUtil.canFollowWorkingDirectory(listOf("fish"), isWindows = false))

        assertFalse(
            TerminalTabsUtil.canFollowWorkingDirectory(listOf("ssh", "romain@buildbox"), isWindows = false),
            "A launcher hides its shell on any host"
        )
    }

    @Test
    fun `follows nothing when the shell is unknown`() {
        assertFalse(TerminalTabsUtil.canFollowWorkingDirectory(null, isWindows = true))
        assertFalse(TerminalTabsUtil.canFollowWorkingDirectory(emptyList(), isWindows = true))
    }

    @Test
    fun `prepends as many parent folders as asked for`() {
        val path = "/home/romain/workspace/TerminalTabTailor/src"

        assertEquals("src", TerminalTabsUtil.directoryNameOf(path, parents = 0))
        assertEquals("TerminalTabTailor/src", TerminalTabsUtil.directoryNameOf(path, parents = 1))
        assertEquals("workspace/TerminalTabTailor/src", TerminalTabsUtil.directoryNameOf(path, parents = 2))
    }

    /** Joined with '/' whatever the shell reports, so a tab reads the same on every platform. */
    @Test
    fun `joins windows parents with a forward slash`() {
        assertEquals(
            "workspace/TerminalTabTailor",
            TerminalTabsUtil.directoryNameOf("C:\\workspace\\TerminalTabTailor", parents = 1)
        )
    }

    @Test
    fun `shows every segment it has when there are fewer parents than asked for`() {
        assertEquals("home/romain", TerminalTabsUtil.directoryNameOf("/home/romain", parents = 5))
        assertEquals("C:", TerminalTabsUtil.directoryNameOf("C:\\", parents = 3))
        assertEquals("/", TerminalTabsUtil.directoryNameOf("/", parents = 3))
    }

    /** The setting is a bounded text field, but a hand-edited XML state must not break naming. */
    @Test
    fun `treats a negative parent count as none`() {
        assertEquals("src", TerminalTabsUtil.directoryNameOf("/home/romain/src", parents = -3))
    }

    @Test
    fun `leaves the name alone when the date option is off`() {
        val settings = TerminalTabTailorSettings().apply { useCurrentDate = false }

        assertEquals("src", TerminalTabsUtil.appendDate("src", settings))
    }

    @Test
    fun `appends the date in the shape the descending sort parses back out`() {
        val settings = TerminalTabTailorSettings().apply {
            useCurrentDate = true
            dateTemplate = "dd-MM-yy"
        }
        val now = SimpleDateFormat("dd-MM-yy").parse("03-04-24")

        assertEquals("src <03-04-24>", TerminalTabsUtil.appendDate("src", settings, now))
    }

    @Test
    fun `appends the date using the configured template`() {
        val settings = TerminalTabTailorSettings().apply {
            useCurrentDate = true
            dateTemplate = "yyyy-MM-dd"
        }
        val now = SimpleDateFormat("dd-MM-yy").parse("03-04-24")

        assertEquals("src <2024-04-03>", TerminalTabsUtil.appendDate("src", settings, now))
    }

    // --- Following a shell honours the naming style ------------------------------------------

    /** A shell has no file to be named after, so this style is left static, like it always was. */
    @Test
    fun `FILE_NAME does not follow the shell`() {
        assertNull(
            TerminalTabsUtil.followedName(
                TabNameTypeEnum.FILE_NAME,
                "/home/romain/workspace/proj/api/src",
                parents = 1
            )
        )
    }

    /** The same string wherever the shell walks, so there is nothing to recompute. */
    @Test
    fun `PROJECT_NAME does not follow the shell`() {
        assertNull(
            TerminalTabsUtil.followedName(TabNameTypeEnum.PROJECT_NAME, "/home/romain/workspace/proj/api")
        )
    }

    /**
     * "Follow the shell's current folder" is CURRENT_DIRECTORY_NAME's own naming style now, not a
     * behaviour FIRST_DIR_NAME gains conditionally — so this style is static like the rest.
     */
    @Test
    fun `FIRST_DIR_NAME does not follow the shell`() {
        assertNull(
            TerminalTabsUtil.followedName(
                TabNameTypeEnum.FIRST_DIR_NAME,
                "/home/romain/workspace/proj/api/src",
                parents = 1
            )
        )
    }

    /** Module-aware following no longer exists: these two styles are static again. */
    @Test
    fun `MODULE_NAME does not follow the shell`() {
        assertNull(
            TerminalTabsUtil.followedName(
                TabNameTypeEnum.MODULE_NAME,
                "/home/romain/workspace/proj/api/src",
                parents = 1
            )
        )
    }

    @Test
    fun `MODULE_DIR_NAME does not follow the shell`() {
        assertNull(
            TerminalTabsUtil.followedName(
                TabNameTypeEnum.MODULE_DIR_NAME,
                "/home/romain/workspace/proj/api/src",
                parents = 1
            )
        )
    }

    /** The one style following actually renames a tab for: the folder itself. */
    @Test
    fun `CURRENT_DIRECTORY_NAME follows the directory itself`() {
        assertEquals(
            "api/src",
            TerminalTabsUtil.followedName(
                TabNameTypeEnum.CURRENT_DIRECTORY_NAME,
                "/home/romain/workspace/proj/api/src",
                parents = 1
            )
        )
    }
}
