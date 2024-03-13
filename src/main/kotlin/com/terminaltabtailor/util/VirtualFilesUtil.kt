package com.terminaltabtailor.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.terminaltabtailor.dto.VirtualSelection

class VirtualFilesUtil {
    companion object {

        /*
         * Could be achieved with: Paths.get(parentModule?.moduleFilePath).parent?.fileName?.toString()
         * But it avoids this warning: 'getModuleFilePath()' is marked unstable with @ApiStatus.Internal
         */
        fun getModuleDirectoryName(module: Module?): String? {
            val contentRoots = module?.let { ModuleRootManager.getInstance(it).contentRoots }
            val moduleDir = contentRoots?.firstOrNull()?.path
            return moduleDir?.substringAfterLast('/')
        }

        fun getVirtualSelection(
            project: Project,
            selectedFile: VirtualFile
        ): VirtualSelection {
            val virtualSelection = VirtualSelection()

            virtualSelection.lastSelectedVirtualFile = selectedFile

            virtualSelection.let {

                virtualSelection.lastSelectedVirtualFile?.let {

                    virtualSelection.lastSelectedVirtualFileParent =
                        virtualSelection.lastSelectedVirtualFile!!.parent

                    ApplicationManager.getApplication().runReadAction {
                        runCatching {
                            val module = ModuleUtilCore.findModuleForFile(
                                virtualSelection.lastSelectedVirtualFile!!,
                                project
                            )
                            virtualSelection.lastSelectedVirtualFileParentModule = module


                            virtualSelection.lastSelectedVirtualFileParentModuleDirName =
                                getModuleDirectoryName(
                                    virtualSelection.lastSelectedVirtualFileParentModule
                                )
                        }
                    }

                }
            }
            return virtualSelection
        }
    }
}