package com.terminaltabtailor.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.terminaltabtailor.manager.TerminalTabNamesManager
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import org.jetbrains.plugins.terminal.exp.TerminalPromotedDumbAwareAction
import java.util.*
import javax.swing.Icon

open class CustomTerminalNewTabAction(
    val text: String = ResourceBundle.getBundle("TerminalTabTailorBundle")
        .getString("action.customTerminalNewTab.text"),
    private val description: String = ResourceBundle.getBundle("TerminalTabTailorBundle")
        .getString("action.customTerminalNewTab.description"),
    private val icon: Icon = AllIcons.General.Add,
) : TerminalPromotedDumbAwareAction() {

    init {
        templatePresentation.also {
            it.text = text
            it.setDescription(description)
            it.setIconSupplier { icon }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val toolWindow = e.dataContext.getData(PlatformDataKeys.TOOL_WINDOW)
        e.presentation.isEnabled = TerminalToolWindowManager.isTerminalToolWindow(toolWindow) && e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        TerminalTabNamesManager.openTabInTerminal(e)
    }

    companion object {
        const val ACTION_ID: String = "Terminal.NewTerminalTabTailorTab"
    }
}