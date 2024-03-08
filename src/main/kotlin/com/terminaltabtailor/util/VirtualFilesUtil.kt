package com.terminaltabtailor.util

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.terminaltabtailor.listeners.TerminalActionListener
import javax.swing.JTree

class VirtualFilesUtil {
    companion object {
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

        private fun getJTreeIfActive(event: AnActionEvent): JTree? {
            val component = PlatformDataKeys.CONTEXT_COMPONENT.getData(event.dataContext)
            return if (component is JTree) component else null
        }

        fun getVirtualSelection(
            project: Project,
            event: AnActionEvent
        ): TerminalActionListener.VirtualSelection {
            val virtualSelection = TerminalActionListener

            getJTreeIfActive(event)?.let { tree ->
                var virtualFiles: Array<out VirtualFile>? = arrayOf()

                val dataContext: DataContext = DataManager.getInstance().getDataContext(tree)

                /*
                * slow operations are not allowed from the EDT, but this must be called from the EDT
                * --> would definitely accept suggestions to improve this part
                */
                WriteCommandAction.writeCommandAction(project).run<Throwable> {
                    virtualFiles = getVirtualFilesFromContext(dataContext)
                }

                virtualFiles?.firstOrNull().let { virtualFile ->
                    virtualSelection.lastSelectedVirtualFile = virtualFile

                    virtualSelection.let {

                        virtualSelection.lastSelectedVirtualFile?.let {

                            virtualSelection.lastSelectedVirtualFileParent =
                                virtualSelection.lastSelectedVirtualFile!!.parent

                            /*
                            * findModuleForFile as a slow operation is not allowed from the EDT
                            * --> would definitely accept suggestions to improve this part
                            */
                            WriteCommandAction.writeCommandAction(project).run<Throwable> {
                                virtualSelection.lastSelectedVirtualFileParentModule =
                                    ModuleUtilCore.findModuleForFile(
                                        virtualSelection.lastSelectedVirtualFile!!,
                                        project
                                    )
                            }

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