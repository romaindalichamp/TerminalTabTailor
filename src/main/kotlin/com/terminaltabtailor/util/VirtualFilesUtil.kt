package com.terminaltabtailor.util

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JTree

class VirtualFilesUtil {
    companion object {
        fun getDataContextFromTree(tree: JTree): DataContext {
            return DataManager.getInstance().getDataContext(tree)
        }

        fun getVirtualFilesFromContext(dataContext: DataContext): Array<out VirtualFile>? {
            return CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        }

        /*
         * Could be achieved with: Paths.get(parentModule?.moduleFilePath).parent?.fileName?.toString()
         * But it avoids this warning: 'getModuleFilePath()' is marked unstable with @ApiStatus.Internal
         */
        fun getModuleDirectoryName(module: Module?): String? {
            val contentRoots = module?.let { ModuleRootManager.getInstance(it).contentRoots }
            val moduleDir = contentRoots?.firstOrNull()?.path
            return moduleDir?.substringAfterLast('/')
        }
    }
}