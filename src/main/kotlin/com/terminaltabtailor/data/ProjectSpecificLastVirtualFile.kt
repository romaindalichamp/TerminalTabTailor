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
        private val settingsService by lazy {
            service<TerminalTabTailorSettingsService>()
        }

        private val lastVirtualFileForProject = WeakHashMap<Project, VirtualFile?>()
        private val lastFEMFileForProject = WeakHashMap<Project, VirtualFile?>()
        private val lastPTVFileForProject = WeakHashMap<Project, VirtualFile?>()

        /*
         * `origin` is a parameter rather than a read of the global service so that the routing
         * between the per-origin maps can be exercised without a running application.
         * Production callers omit it.
         */
        fun getLastVirtualFileForProject(
            project: Project,
            origin: TabNameOriginEnum = settingsService.state.selectedTabOrigin
        ): VirtualFile? {
            return when (origin) {
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