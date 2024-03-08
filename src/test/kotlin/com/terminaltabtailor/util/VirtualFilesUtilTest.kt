package com.terminaltabtailor.util

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mockStatic
import javax.swing.JTree
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VirtualFilesUtilTest {
    @Test
    fun testGetVirtualFilesFromContext() {
        val dataContext = mock(DataContext::class.java)
        val virtualFiles = arrayOf(mock(VirtualFile::class.java))

        `when`(CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)).thenReturn(virtualFiles)

        val result = VirtualFilesUtil.getVirtualFilesFromContext(dataContext)
        assertTrue(result?.isNotEmpty() == true)
    }

    @Test
    fun testGetModuleDirectoryName() {
        val module = mock(Module::class.java)
        val moduleRootManager = mock(ModuleRootManager::class.java)
        val virtualFile = mock(VirtualFile::class.java)
        val path = "/path/to/moduleDir"

        `when`(virtualFile.path).thenReturn(path)
        `when`(moduleRootManager.contentRoots).thenReturn(arrayOf(virtualFile))
        `when`(ModuleRootManager.getInstance(module)).thenReturn(moduleRootManager)

        val result = VirtualFilesUtil.getModuleDirectoryName(module)
        assertEquals("moduleDir", result)
    }
}