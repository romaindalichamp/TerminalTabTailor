package com.terminaltabtailor.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.terminaltabtailor.enums.TabNameSort
import com.terminaltabtailor.enums.TabNameType
import com.terminaltabtailor.util.TerminalTabsUtil
import java.util.*

class TerminalTabTailorConfigurable(private val project: Project) :
    BoundConfigurable(ResourceBundle.getBundle("TerminalTabTailorBundle").getString("settings.displayName")) {

    private val settingsService = service<TerminalTabTailorSettingsService>()
    private val bundle: ResourceBundle = ResourceBundle.getBundle("TerminalTabTailorBundle")

    override fun apply() {
        super.apply()
        TerminalTabsUtil.getTerminalToolWindow(project)?.contentManager?.let {
            TerminalTabsUtil.sortTabs(it, settingsService)
            TerminalTabsUtil.activateTerminalWindow(project)
        }
    }

    override fun createPanel(): DialogPanel {
        return panel {
            group(bundle.getString("settings.options.title")) {
                row {
                    checkBox(bundle.getString("settings.alreadyExists.text")).bindSelected(settingsService.state::alreadyExists)
                }
                row {
                    checkBox(bundle.getString("settings.performManualRenaming.text")).bindSelected(settingsService.state::performManualRenaming)
                }
                row {
                    checkBox(bundle.getString("settings.useCurrentDate.text")).bindSelected(settingsService.state::useCurrentDate)
                }
                row(bundle.getString("settings.dateTemplate.text")) {
                    textField().bindText(settingsService.state::dateTemplate)
                }
            }

            group(bundle.getString("settings.sort.label")) {
                buttonsGroup(title = bundle.getString("settings.sort.title")) {
                    row { radioButton(bundle.getString("settings.asc.text"), TabNameSort.ASC) }
                    row { radioButton(bundle.getString("settings.desc_date.text"), TabNameSort.DESC_DATE) }
                    row { radioButton(bundle.getString("settings.no_sort.text"), TabNameSort.NO_SORT) }
                }.bind(settingsService.state::selectedTabTypeSort)
            }

            group(bundle.getString("settings.name.label")) {
                buttonsGroup(title = bundle.getString("settings.name.title")) {
                    row { radioButton(bundle.getString("settings.file_name.text"), TabNameType.FILE_NAME) }
                    row { radioButton(bundle.getString("settings.first_dir_name.text"), TabNameType.FIRST_DIR_NAME) }
                    row { radioButton(bundle.getString("settings.module_name.text"), TabNameType.MODULE_NAME) }
                    row { radioButton(bundle.getString("settings.module_dir_name.text"), TabNameType.MODULE_DIR_NAME) }
                    row { radioButton(bundle.getString("settings.project_name.text"), TabNameType.PROJECT_NAME) }

                }.bind(settingsService.state::selectedTabTypeName)
            }

            group(bundle.getString("settings.project.title")) {
                row {
                    text(bundle.getString("settings.project.text")).component.also {
                            it.isEditable = false
                        }
                }
            }
        }
    }
}
