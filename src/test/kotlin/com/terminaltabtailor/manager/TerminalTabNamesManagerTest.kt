package com.terminaltabtailor.manager

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.terminaltabtailor.enum.TabNameTypeEnum
import com.terminaltabtailor.settings.TerminalTabTailorSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.*

/**
 * Covers the naming rules a user picks in "Naming style", plus the optional date suffix.
 * These decide what every terminal tab ends up called, so each style and each fallback is pinned.
 */
class TerminalTabNamesManagerTest {

    private val project = mock(Project::class.java).also {
        `when`(it.name).thenReturn("MyProject")
    }

    /** 3 April 2024, so the default template renders as the familiar `03-04-24`. */
    private val fixedDate: Date = GregorianCalendar(2024, Calendar.APRIL, 3).time

    private fun settings(
        type: TabNameTypeEnum,
        useCurrentDate: Boolean = false,
        dateTemplate: String = "dd-MM-yy"
    ) = TerminalTabTailorSettings().apply {
        this.selectedTabTypeName = type
        this.useCurrentDate = useCurrentDate
        this.dateTemplate = dateTemplate
    }

    private fun file(name: String) = mock(VirtualFile::class.java).also {
        `when`(it.name).thenReturn(name)
        `when`(it.isValid).thenReturn(true)
        `when`(it.isDirectory).thenReturn(false)
    }

    private fun directory(name: String) = mock(VirtualFile::class.java).also {
        `when`(it.name).thenReturn(name)
        `when`(it.isValid).thenReturn(true)
        `when`(it.isDirectory).thenReturn(true)
    }

    private fun module(name: String) = mock(Module::class.java).also {
        `when`(it.name).thenReturn(name)
    }

    private fun nameFor(
        settings: TerminalTabTailorSettings,
        selected: VirtualFile,
        parent: VirtualFile? = null,
        parentModule: Module? = null,
        parentModuleDirName: String? = null
    ) = TerminalTabNamesManager.constructNewTabName(
        project, selected, parent, parentModule, parentModuleDirName, settings, fixedDate
    )

    // --- Naming style: file name -------------------------------------------------------------

    @Test
    fun `FILE_NAME uses the selected file's own name`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FILE_NAME),
            file("Main.kt"),
            parent = directory("controllers")
        )

        assertEquals("Main.kt", result)
    }

    // --- Naming style: first directory name --------------------------------------------------

    @Test
    fun `FIRST_DIR_NAME uses the parent directory when a file is selected`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FIRST_DIR_NAME),
            file("Main.kt"),
            parent = directory("controllers")
        )

        assertEquals("controllers", result)
    }

    @Test
    fun `FIRST_DIR_NAME uses the directory itself when a directory is selected`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FIRST_DIR_NAME),
            directory("controllers"),
            parent = directory("src")
        )

        assertEquals("controllers", result)
    }

    @Test
    fun `FIRST_DIR_NAME falls back to the project name when a file has no parent`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FIRST_DIR_NAME),
            file("Main.kt"),
            parent = null
        )

        assertEquals("MyProject", result)
    }

    // --- Naming style: module name -----------------------------------------------------------

    @Test
    fun `MODULE_NAME uses the parent module`() {
        val result = nameFor(
            settings(TabNameTypeEnum.MODULE_NAME),
            file("Main.kt"),
            parentModule = module("core")
        )

        assertEquals("core", result)
    }

    @Test
    fun `MODULE_NAME falls back to the project name outside any module`() {
        val result = nameFor(
            settings(TabNameTypeEnum.MODULE_NAME),
            file("Main.kt"),
            parentModule = null
        )

        assertEquals("MyProject", result)
    }

    // --- Naming style: module directory name -------------------------------------------------

    @Test
    fun `MODULE_DIR_NAME uses the module directory`() {
        val result = nameFor(
            settings(TabNameTypeEnum.MODULE_DIR_NAME),
            file("Main.kt"),
            parentModule = module("core"),
            parentModuleDirName = "core-impl"
        )

        assertEquals("core-impl", result)
    }

    @Test
    fun `MODULE_DIR_NAME falls back to the module name when the directory is unknown`() {
        val result = nameFor(
            settings(TabNameTypeEnum.MODULE_DIR_NAME),
            file("Main.kt"),
            parentModule = module("core"),
            parentModuleDirName = null
        )

        assertEquals("core", result)
    }

    @Test
    fun `MODULE_DIR_NAME falls back to the project name with neither module nor directory`() {
        val result = nameFor(
            settings(TabNameTypeEnum.MODULE_DIR_NAME),
            file("Main.kt"),
            parentModule = null,
            parentModuleDirName = null
        )

        assertEquals("MyProject", result)
    }

    // --- Naming style: project name ----------------------------------------------------------

    @Test
    fun `PROJECT_NAME always uses the project name`() {
        val result = nameFor(
            settings(TabNameTypeEnum.PROJECT_NAME),
            file("Main.kt"),
            parent = directory("controllers"),
            parentModule = module("core"),
            parentModuleDirName = "core-impl"
        )

        assertEquals("MyProject", result)
    }

    // --- Date suffix -------------------------------------------------------------------------

    @Test
    fun `date is appended in angle brackets using the default template`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FIRST_DIR_NAME, useCurrentDate = true),
            file("Main.kt"),
            parent = directory("controllers")
        )

        assertEquals("controllers <03-04-24>", result)
    }

    @Test
    fun `date honours a custom template`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FIRST_DIR_NAME, useCurrentDate = true, dateTemplate = "yyyy_MM_dd"),
            file("Main.kt"),
            parent = directory("controllers")
        )

        assertEquals("controllers <2024_04_03>", result)
    }

    @Test
    fun `no suffix is added when the date option is off`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FIRST_DIR_NAME, useCurrentDate = false),
            file("Main.kt"),
            parent = directory("controllers")
        )

        assertEquals("controllers", result)
    }
}
