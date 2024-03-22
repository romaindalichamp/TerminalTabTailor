package com.terminaltabtailor.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals


class VirtualSelectionUtilTest {

    @Test
    fun testGetModuleDirectoryName() {
        val module = mock(Module::class.java)
        val moduleRootManager = mock(ModuleRootManager::class.java)
        val virtualFile = mock(VirtualFile::class.java)
        val path = "/path/to/moduleDir"

        `when`(virtualFile.path).thenReturn(path)
        `when`(moduleRootManager.contentRoots).thenReturn(arrayOf(virtualFile))
        `when`(ModuleRootManager.getInstance(module)).thenReturn(moduleRootManager)

        val result = VirtualSelectionUtil.getModuleDirectoryName(module)

        assertEquals("moduleDir", result)
    }

//    @Test
//    fun testGetSelectedFileWithDirectVirtualFile() {
//        val event = mock(AnActionEvent::class.java)
//        val virtualFile = mock(VirtualFile::class.java)
//
//        `when`(event.getData(CommonDataKeys.VIRTUAL_FILE)).thenReturn(virtualFile)
//
//        val result: VirtualFile? = VirtualSelectionUtil.getSelectedFile(event)
//
//        assertEquals(virtualFile, result)
//    }

//    @Test
//    fun testGetSelectedFileWithProjectTreeSelectionListenerVirtualFile() {
//        val event = mock(AnActionEvent::class.java)
//        val virtualFile = mock(VirtualFile::class.java)
//        ProjectTreeSelectionListener.virtualFile = virtualFile
//
//        `when`(event.getData(CommonDataKeys.VIRTUAL_FILE)).thenReturn(null)
//
//        val result: VirtualFile? = VirtualSelectionUtil.getSelectedFile(event)
//
//        assertEquals(virtualFile, result)
//    }

//    @Test
//    fun testGetSelectedFileWithProjectGuessProjectDir() {
//        val event = mock(AnActionEvent::class.java)
//        val virtualFile = mock(VirtualFile::class.java)
//        val project: Project = project
//        ProjectTreeSelectionListener.virtualFile = null
//
//        `when`(event.getData(CommonDataKeys.VIRTUAL_FILE)).thenReturn(null)
//        `when`(event.project).thenReturn(project)
//        `when`(project.guessProjectDir()).thenReturn(virtualFile)
//
//        val result: VirtualFile? = VirtualSelectionUtil.getSelectedFile(event)
//
//        assertEquals(virtualFile, result)
//    }
//
//    @Test
//    fun testGetSelectedFileWithProjectProjectFile() {
//        val event = mock(AnActionEvent::class.java)
//        val virtualFile = mock(VirtualFile::class.java)
//        val project: Project = mock(Project::class.java)
//        ProjectTreeSelectionListener.virtualFile = null
//
//        `when`(event.getData(CommonDataKeys.VIRTUAL_FILE)).thenReturn(null)
//        `when`(event.project).thenReturn(project)
//        `when`(project.projectFile).thenReturn(virtualFile)
//
//        val result: VirtualFile? = VirtualSelectionUtil.getSelectedFile(event)
//
//        assertEquals(virtualFile, result)
//    }

//    @Test
//    fun testGetSelectedFileReturnsNull() {
//        val event = mock(AnActionEvent::class.java)
//        val project: Project = mock(Project::class.java)
//        ProjectTreeSelectionListener.virtualFile = null
//
//        `when`(event.getData(CommonDataKeys.VIRTUAL_FILE)).thenReturn(null)
//        `when`(event.project).thenReturn(project)
//        `when`(event.project?.isDefault).thenReturn(true)
//        `when`(project.projectFile).thenReturn(null)
//
//        val result = VirtualSelectionUtil.getSelectedFile(event)
//
//        assertNull(result)
//    }

}