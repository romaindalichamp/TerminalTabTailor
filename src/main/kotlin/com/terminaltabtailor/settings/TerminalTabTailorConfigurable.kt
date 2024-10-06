package com.terminaltabtailor.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.terminaltabtailor.enum.TabNameOriginEnum
import com.terminaltabtailor.enum.TabNameSortEnum
import com.terminaltabtailor.enum.TabNameTypeEnum
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
                    row { radioButton(bundle.getString("settings.asc.text"), TabNameSortEnum.ASC) }
                    row { radioButton(bundle.getString("settings.desc_date.text"), TabNameSortEnum.DESC_DATE) }
                    row { radioButton(bundle.getString("settings.no_sort.text"), TabNameSortEnum.NO_SORT) }
                }.bind(settingsService.state::selectedTabTypeSort)
            }

            group(bundle.getString("settings.name.type.label")) {
                buttonsGroup(title = bundle.getString("settings.name.type.title")) {
                    row {
                        radioButton(
                            bundle.getString("settings.name.type.file_name.text"),
                            TabNameTypeEnum.FILE_NAME
                        )
                    }
                    row {
                        radioButton(
                            bundle.getString("settings.name.type.first_dir_name.text"),
                            TabNameTypeEnum.FIRST_DIR_NAME
                        )
                    }
                    row {
                        radioButton(
                            bundle.getString("settings.name.type.module_name.text"),
                            TabNameTypeEnum.MODULE_NAME
                        )
                    }
                    row {
                        radioButton(
                            bundle.getString("settings.name.type.module_dir_name.text"),
                            TabNameTypeEnum.MODULE_DIR_NAME
                        )
                    }
                    row {
                        radioButton(
                            bundle.getString("settings.name.type.project_name.text"),
                            TabNameTypeEnum.PROJECT_NAME
                        )
                    }
                }.bind(settingsService.state::selectedTabTypeName)
            }

            group(bundle.getString("settings.name.origin.label")) {
                buttonsGroup(title = bundle.getString("settings.name.origin.title")) {
                    row {
                        radioButton(
                            bundle.getString("settings.name.origin.project_tree_view.text"),
                            TabNameOriginEnum.PROJECT_TREE_VIEW
                        )
                    }
                    row {
                        radioButton(
                            bundle.getString("settings.name.origin.file_editor_manager.text"),
                            TabNameOriginEnum.FILE_EDITOR_MANAGER
                        )
                    }
                    row {
                        radioButton(
                            bundle.getString("settings.name.origin.mixed.text"),
                            TabNameOriginEnum.MIXED
                        )
                    }
                    row {
                        radioButton(
                            bundle.getString("settings.name.origin.project.text"),
                            TabNameOriginEnum.PROJECT_NAME
                        )
                    }
                }.bind(settingsService.state::selectedTabOrigin)
            }

            group(bundle.getString("settings.project.title")) {
                row {
                    text(bundle.getString("settings.project.text"))
                }
            }
        }
    }
}
