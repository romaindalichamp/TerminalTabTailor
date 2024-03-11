package com.terminaltabtailor.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals

class VirtualFilesUtilTest {

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