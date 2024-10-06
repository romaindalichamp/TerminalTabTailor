package com.terminaltabtailor.data

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.terminaltabtailor.enum.TabNameOriginEnum
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import java.util.*

class ProjectSpecificLastVirtualFile(val project: Project) {
    companion object {
        private val lastVirtualFileForProject = WeakHashMap<Project, VirtualFile?>()
        private val lastFEMFileForProject = WeakHashMap<Project, VirtualFile?>()
        private val lastPTVFileForProject = WeakHashMap<Project, VirtualFile?>()

        fun getLastVirtualFileForProject(project: Project): VirtualFile? {
            return when (service<TerminalTabTailorSettingsService>().state.selectedTabOrigin) {
                TabNameOriginEnum.FILE_EDITOR_MANAGER -> lastFEMFileForProject[project]
                TabNameOriginEnum.PROJECT_TREE_VIEW -> lastPTVFileForProject[project]
                TabNameOriginEnum.MIXED -> lastVirtualFileForProject[project]
                TabNameOriginEnum.PROJECT_NAME -> project.guessProjectDir()
            }
        }

        fun setLastVirtualFileForProject(project: Project, file: VirtualFile?, tabNameOrigin: TabNameOriginEnum) {
            when (tabNameOrigin) {
                TabNameOriginEnum.FILE_EDITOR_MANAGER -> lastFEMFileForProject[project] = file
                TabNameOriginEnum.PROJECT_TREE_VIEW -> lastPTVFileForProject[project] = file
                TabNameOriginEnum.PROJECT_NAME -> return
                TabNameOriginEnum.MIXED -> {
                    lastFEMFileForProject[project] = file
                    lastPTVFileForProject[project] = file
                }
            }
            lastVirtualFileForProject[project] = file
        }
    }
}