package com.terminaltabtailor.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "TerminalTabTailorSettings",
    storages = [Storage("TerminalTabTailorSettings.xml")]
)
class TerminalTabTailorSettingsService : PersistentStateComponent<TerminalTabTailorSettings> {
    private var settings = TerminalTabTailorSettings()

    override fun getState(): TerminalTabTailorSettings {
        return settings
    }

    override fun loadState(state: TerminalTabTailorSettings) {
        settings = state
    }
}
