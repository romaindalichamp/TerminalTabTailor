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
        dateTemplate: String = "dd-MM-yy",
        parents: Int = 0
    ) = TerminalTabTailorSettings().apply {
        this.selectedTabTypeName = type
        this.useCurrentDate = useCurrentDate
        this.dateTemplate = dateTemplate
        this.currentDirectoryParents = parents
    }

    /**
     * `path` matters as soon as parent folders are asked for; a mock that leaves it unstubbed falls
     * back to the plain name, which is what the default of no parents produces anyway.
     */
    private fun file(name: String, path: String? = null) = mock(VirtualFile::class.java).also {
        `when`(it.name).thenReturn(name)
        `when`(it.path).thenReturn(path)
        `when`(it.isValid).thenReturn(true)
        `when`(it.isDirectory).thenReturn(false)
    }

    private fun directory(name: String, path: String? = null) = mock(VirtualFile::class.java).also {
        `when`(it.name).thenReturn(name)
        `when`(it.path).thenReturn(path)
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
        parentModuleDirPath: String? = null
    ) = TerminalTabNamesManager.constructNewTabName(
        project, selected, parent, parentModule, parentModuleDirPath, settings, fixedDate
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
            parentModuleDirPath = "/home/romain/workspace/core-impl"
        )

        assertEquals("core-impl", result)
    }

    @Test
    fun `MODULE_DIR_NAME falls back to the module name when the directory is unknown`() {
        val result = nameFor(
            settings(TabNameTypeEnum.MODULE_DIR_NAME),
            file("Main.kt"),
            parentModule = module("core"),
            parentModuleDirPath = null
        )

        assertEquals("core", result)
    }

    @Test
    fun `MODULE_DIR_NAME falls back to the project name with neither module nor directory`() {
        val result = nameFor(
            settings(TabNameTypeEnum.MODULE_DIR_NAME),
            file("Main.kt"),
            parentModule = null,
            parentModuleDirPath = null
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
            parentModuleDirPath = "/home/romain/workspace/core-impl"
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

    // --- Parent folders ----------------------------------------------------------------------

    @Test
    fun `FILE_NAME prepends parent folders`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FILE_NAME, parents = 2),
            file("Main.kt", "/home/romain/app/src/controllers/Main.kt")
        )

        assertEquals("src/controllers/Main.kt", result)
    }

    @Test
    fun `FIRST_DIR_NAME prepends parent folders of the directory it names`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FIRST_DIR_NAME, parents = 1),
            file("Main.kt", "/home/romain/app/src/controllers/Main.kt"),
            parent = directory("controllers", "/home/romain/app/src/controllers")
        )

        assertEquals("src/controllers", result)
    }

    @Test
    fun `FIRST_DIR_NAME prepends parent folders when a directory is selected outright`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FIRST_DIR_NAME, parents = 1),
            directory("controllers", "/home/romain/app/src/controllers")
        )

        assertEquals("src/controllers", result)
    }

    @Test
    fun `MODULE_DIR_NAME prepends parent folders`() {
        val result = nameFor(
            settings(TabNameTypeEnum.MODULE_DIR_NAME, parents = 1),
            file("Main.kt"),
            parentModule = module("core"),
            parentModuleDirPath = "/home/romain/workspace/core-impl"
        )

        assertEquals("workspace/core-impl", result)
    }

    /**
     * A module or project name is not a path, so there is nothing to prepend — asking for parents
     * must leave these two styles exactly as they were.
     */
    @Test
    fun `MODULE_NAME and PROJECT_NAME ignore the parent count`() {
        val moduleName = nameFor(
            settings(TabNameTypeEnum.MODULE_NAME, parents = 3),
            file("Main.kt", "/home/romain/app/src/Main.kt"),
            parentModule = module("core")
        )
        val projectName = nameFor(
            settings(TabNameTypeEnum.PROJECT_NAME, parents = 3),
            file("Main.kt", "/home/romain/app/src/Main.kt")
        )

        assertEquals("core", moduleName)
        assertEquals("MyProject", projectName)
    }

    @Test
    fun `parent folders combine with the date suffix`() {
        val result = nameFor(
            settings(TabNameTypeEnum.FIRST_DIR_NAME, useCurrentDate = true, parents = 1),
            directory("controllers", "/home/romain/app/src/controllers")
        )

        assertEquals("src/controllers <03-04-24>", result)
    }
}
