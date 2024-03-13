package com.terminaltabtailor.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.terminaltabtailor.enums.TabNameSort
import com.terminaltabtailor.enums.TabNameType
import com.terminaltabtailor.util.TerminalTabsUtil
import net.miginfocom.swing.MigLayout
import java.awt.Dimension
import javax.swing.*

class TerminalTabTailorConfigurable(private val project: Project) : Configurable {
    private val settingsService = service<TerminalTabTailorSettingsService>()
    private var useCurrentDate = JBCheckBox("Incorporate the current date into tab names.")
    private var alreadyExists =
        JBCheckBox("Reuse an existing tab when the names match.")

    private val chooseKindOfSortGroup = ButtonGroup()
    private var ascSort = JBRadioButton("Maintain tabs in perpetual alphabetical order.")
    private var descDateSort = JBRadioButton("Maintain tabs sorted by descending date.")

    private var dateTemplate = JBTextField()
    private var performManualRenaming =
        JBCheckBox("Prompt the renaming dialogue each time a new terminal tab is opened.")
    private var dateTemplatePanel = JPanel(MigLayout())

    private val chooseKindOfNameGroup = ButtonGroup()
    private var authoriseFilesName = JBRadioButton("Permit the use of file names directly.")
    private var alwaysParentDirName =
        JBRadioButton("Default to directory names, utilizing the parent directory's name for files.")
    private var alwaysParentModuleName = JBRadioButton("Adopt the name of the parent module.")
    private var alwaysParentModuleDirectoryName =
        JBRadioButton("Use the parent module's `directory` name for a more structured approach.")
    private var alwaysProjectName =
        JBRadioButton("Consistently use the project's name for all terminal tabs.")

    private val settingsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        dateTemplatePanel.maximumSize =
            Dimension(Integer.MAX_VALUE, dateTemplate.getPreferredSize().height + 10)

        chooseKindOfNameGroup.add(authoriseFilesName)
        chooseKindOfNameGroup.add(alwaysParentDirName)
        chooseKindOfNameGroup.add(alwaysParentModuleName)
        chooseKindOfNameGroup.add(alwaysParentModuleDirectoryName)
        chooseKindOfNameGroup.add(alwaysProjectName)

        chooseKindOfSortGroup.add(ascSort)
        chooseKindOfSortGroup.add(descDateSort)

        add(JBLabel("Options"))
        add(Box.createVerticalStrut(15))

        add(performManualRenaming)
        add(alreadyExists)
        add(Box.createVerticalStrut(15))
        add(ascSort)
        add(descDateSort)
        add(Box.createVerticalStrut(15))
        add(useCurrentDate)
        dateTemplatePanel.add(JBLabel("Choose a template to display the date in your tab's name: "))
        dateTemplatePanel.add(dateTemplate)
        add(dateTemplatePanel)

        add(Box.createVerticalStrut(30))
        add(JBLabel("Choose a name type for your tabs"))
        add(Box.createVerticalStrut(15))

        add(authoriseFilesName)
        add(alwaysParentDirName)
        add(alwaysParentModuleName)
        add(alwaysParentModuleDirectoryName)
        add(alwaysProjectName)
        add(Box.createVerticalStrut(15))

        val separator = JSeparator(JSeparator.HORIZONTAL)
        separator.maximumSize = Dimension(Integer.MAX_VALUE, 20)
        add(separator)
        add(JBLabel("Terminal Tab Tailor significantly enhances the IntelliJ IDEA workflow by dynamically renaming "))
        add(JBLabel("terminal tabs to reflect the currently selected item in your project tree. This feature greatly aids in "))
        add(JBLabel("organizing your workspace and navigating effortlessly between numerous terminal sessions."))
        add(JBLabel("Simply select a file or folder within your project tree and initiate a terminal session from that context"))
    }

    override fun getDisplayName() = "Terminal Tab Tailor"

    override fun createComponent(): JComponent {
        useCurrentDate.isSelected = settingsService.state.useCurrentDate
        alreadyExists.isSelected = settingsService.state.alreadyExists
        performManualRenaming.isSelected = settingsService.state.performManualRenaming
        dateTemplate.text = settingsService.state.dateTemplate

        when (settingsService.state.selectedTabTypeSort) {
            TabNameSort.ASC -> ascSort.isSelected = true
            TabNameSort.DESC_DATE -> descDateSort.isSelected = true
        }

        when (settingsService.state.selectedTabTypeName) {
            TabNameType.FILE_NAME -> authoriseFilesName.isSelected = true
            TabNameType.FIRST_DIR_NAME -> alwaysParentDirName.isSelected = true
            TabNameType.MODULE_NAME -> alwaysParentModuleName.isSelected = true
            TabNameType.MODULE_DIR_NAME -> alwaysParentModuleDirectoryName.isSelected = true
            TabNameType.PROJECT_NAME -> alwaysProjectName.isSelected = true
        }

        return settingsPanel
    }

    override fun isModified(): Boolean {
        val selectedTabNameSort = when {
            ascSort.isSelected -> TabNameSort.ASC
            descDateSort.isSelected -> TabNameSort.DESC_DATE
            else -> TabNameSort.ASC
        }

        val selectedTabTypeName = when {
            authoriseFilesName.isSelected -> TabNameType.FILE_NAME
            alwaysParentDirName.isSelected -> TabNameType.FIRST_DIR_NAME
            alwaysParentModuleName.isSelected -> TabNameType.MODULE_NAME
            alwaysParentModuleDirectoryName.isSelected -> TabNameType.MODULE_DIR_NAME
            alwaysProjectName.isSelected -> TabNameType.PROJECT_NAME
            else -> TabNameType.MODULE_NAME
        }

        return useCurrentDate.isSelected != settingsService.state.useCurrentDate
                || alreadyExists.isSelected != settingsService.state.alreadyExists
                || performManualRenaming.isSelected != settingsService.state.performManualRenaming
                || dateTemplate.text != settingsService.state.dateTemplate
                || selectedTabNameSort != settingsService.state.selectedTabTypeSort
                || selectedTabTypeName != settingsService.state.selectedTabTypeName
    }

    override fun apply() {
        settingsService.state.useCurrentDate = useCurrentDate.isSelected
        settingsService.state.alreadyExists = alreadyExists.isSelected
        settingsService.state.performManualRenaming = performManualRenaming.isSelected
        settingsService.state.dateTemplate = dateTemplate.text

        settingsService.state.selectedTabTypeSort = when {
            ascSort.isSelected -> TabNameSort.ASC
            descDateSort.isSelected -> TabNameSort.DESC_DATE
            else -> TabNameSort.ASC
        }

        settingsService.state.selectedTabTypeName = when {
            authoriseFilesName.isSelected -> TabNameType.FILE_NAME
            alwaysParentDirName.isSelected -> TabNameType.FIRST_DIR_NAME
            alwaysParentModuleName.isSelected -> TabNameType.MODULE_NAME
            alwaysParentModuleDirectoryName.isSelected -> TabNameType.MODULE_DIR_NAME
            alwaysProjectName.isSelected -> TabNameType.PROJECT_NAME
            else -> TabNameType.MODULE_NAME
        }

        TerminalTabsUtil.getTerminalToolWindow(project)?.contentManager?.let {
            TerminalTabsUtil.sortTabs(it, settingsService)
            TerminalTabsUtil.activateTerminalWindow(project)
        }
    }
}
