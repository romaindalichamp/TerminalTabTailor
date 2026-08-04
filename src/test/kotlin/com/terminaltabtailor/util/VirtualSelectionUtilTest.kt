package com.terminaltabtailor.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`

class VirtualSelectionUtilTest {

    /*
     * ModuleRootManager.getInstance is static, so it needs mockStatic. Stubbing it with a plain
     * when(...) used to appear to work because the call happened to delegate to module.getComponent,
     * which Mockito captured as an invocation on the mock. That stopped being true in 2025.3, where
     * it resolves through ProjectRootManager instead.
     */
    @Test
    fun `module directory name is the last segment of the first content root`() {
        val module = mock(Module::class.java)
        val moduleRootManager = mock(ModuleRootManager::class.java)
        val contentRoot = mock(VirtualFile::class.java)

        `when`(contentRoot.path).thenReturn("/path/to/moduleDir")
        `when`(moduleRootManager.contentRoots).thenReturn(arrayOf(contentRoot))

        mockStatic(ModuleRootManager::class.java).use { staticMock ->
            staticMock.`when`<ModuleRootManager> { ModuleRootManager.getInstance(module) }
                .thenReturn(moduleRootManager)

            assertEquals("moduleDir", VirtualSelectionUtil.getModuleDirectoryName(module))
        }
    }

    @Test
    fun `module directory name is null without a module`() {
        assertNull(VirtualSelectionUtil.getModuleDirectoryName(null))
    }

    @Test
    fun `module directory name is null when the module has no content root`() {
        val module = mock(Module::class.java)
        val moduleRootManager = mock(ModuleRootManager::class.java)

        `when`(moduleRootManager.contentRoots).thenReturn(emptyArray())

        mockStatic(ModuleRootManager::class.java).use { staticMock ->
            staticMock.`when`<ModuleRootManager> { ModuleRootManager.getInstance(module) }
                .thenReturn(moduleRootManager)

            assertNull(VirtualSelectionUtil.getModuleDirectoryName(module))
        }
    }
}
