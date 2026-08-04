package com.terminaltabtailor.data

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.terminaltabtailor.enum.TabNameOriginEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/**
 * Covers which selection the TTT button follows ("TTT Button" in the settings).
 *
 * The project tree and the file editor are recorded into separate maps, and the "most recent"
 * origin is a third map written by both. Each test uses its own mock project, so the shared
 * per-project state cannot leak between them.
 *
 * The PROJECT_NAME branch of the getter is not asserted here: it resolves the project directory
 * through the platform rather than these maps, so it needs a real project to be meaningful.
 */
class ProjectSpecificLastVirtualFileTest {

    private fun project() = mock(Project::class.java)

    private fun file() = mock(VirtualFile::class.java)

    @Test
    fun `project tree selection is readable from the project tree origin`() {
        val project = project()
        val selected = file()

        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            project, selected, TabNameOriginEnum.PROJECT_TREE_VIEW
        )

        assertEquals(
            selected,
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.PROJECT_TREE_VIEW
            )
        )
    }

    @Test
    fun `file editor selection is readable from the file editor origin`() {
        val project = project()
        val selected = file()

        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            project, selected, TabNameOriginEnum.FILE_EDITOR_MANAGER
        )

        assertEquals(
            selected,
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.FILE_EDITOR_MANAGER
            )
        )
    }

    @Test
    fun `the two origins stay isolated from each other`() {
        val project = project()
        val fromTree = file()

        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            project, fromTree, TabNameOriginEnum.PROJECT_TREE_VIEW
        )

        assertNull(
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.FILE_EDITOR_MANAGER
            ),
            "A project tree selection must not show up as a file editor selection"
        )
    }

    @Test
    fun `a single origin write is still visible as the most recent selection`() {
        val project = project()
        val fromTree = file()

        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            project, fromTree, TabNameOriginEnum.PROJECT_TREE_VIEW
        )

        assertEquals(
            fromTree,
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.MIXED
            ),
            "MIXED follows whichever source moved last"
        )
    }

    @Test
    fun `a mixed write populates every origin`() {
        val project = project()
        val selected = file()

        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            project, selected, TabNameOriginEnum.MIXED
        )

        assertEquals(
            selected,
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.PROJECT_TREE_VIEW
            )
        )
        assertEquals(
            selected,
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.FILE_EDITOR_MANAGER
            )
        )
        assertEquals(
            selected,
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.MIXED
            )
        )
    }

    @Test
    fun `the most recent selection tracks the latest write across origins`() {
        val project = project()
        val fromTree = file()
        val fromEditor = file()

        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            project, fromTree, TabNameOriginEnum.PROJECT_TREE_VIEW
        )
        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            project, fromEditor, TabNameOriginEnum.FILE_EDITOR_MANAGER
        )

        assertEquals(
            fromEditor,
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.MIXED
            )
        )
        assertEquals(
            fromTree,
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.PROJECT_TREE_VIEW
            ),
            "The per-origin record must survive a write from the other origin"
        )
    }

    @Test
    fun `a project name write records nothing and leaves the most recent selection alone`() {
        val project = project()
        val fromTree = file()
        val ignored = file()

        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            project, fromTree, TabNameOriginEnum.PROJECT_TREE_VIEW
        )
        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            project, ignored, TabNameOriginEnum.PROJECT_NAME
        )

        assertEquals(
            fromTree,
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                project, TabNameOriginEnum.MIXED
            ),
            "PROJECT_NAME returns before recording, so it must not overwrite the last selection"
        )
    }

    @Test
    fun `projects do not share selections`() {
        val projectA = project()
        val projectB = project()
        val selected = file()

        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            projectA, selected, TabNameOriginEnum.MIXED
        )

        assertNull(
            ProjectSpecificLastVirtualFile.getLastVirtualFileForProject(
                projectB, TabNameOriginEnum.MIXED
            ),
            "Selections are per project"
        )
    }
}
