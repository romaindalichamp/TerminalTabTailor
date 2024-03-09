package com.terminaltabtailor.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.TerminalTabsUtil
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

class TerminalTabsListener(private val project: Project) : ContentManagerListener {
    private val settingsService = service<TerminalTabTailorSettingsService>()

    private val propertyChangeListener = PropertyChangeListener { evt: PropertyChangeEvent ->
        if ("displayName" == evt.propertyName) {
            TerminalTabsUtil.sortTabs(project, settingsService)
            TerminalTabsUtil.forceSelectNewTab(project, evt.newValue.toString())
            TerminalTabsUtil.activateTerminalWindow(project)
        }
    }

    override fun contentAdded(event: ContentManagerEvent) {
        event.content.addPropertyChangeListener(propertyChangeListener)
    }

    override fun contentRemoved(event: ContentManagerEvent) {
        event.content.removePropertyChangeListener(propertyChangeListener)
    }

    // Implement other required methods from ContentManagerListener interface
    override fun selectionChanged(event: ContentManagerEvent) {}
    override fun contentRemoveQuery(event: ContentManagerEvent) {}
}