package com.terminaltabtailor.action

class ActionId {
    companion object {
        const val TERMINAL_RENAME_SESSION_ID = "Terminal.RenameSession"
        const val OPEN_IN_TERMINAL_ID = "Terminal.OpenInTerminal"

        /**
         * Registered by the platform from 2025.3 alongside [OPEN_IN_TERMINAL_ID]: one action per
         * terminal engine, each visible only when its engine is the selected one.
         */
        const val OPEN_IN_REWORKED_TERMINAL_ID = "Terminal.OpenInReworkedTerminal"
        const val RENAME_SESSION_LABEL_ID = "action.RenameSession.newSessionName.label"
        const val OPEN_IN_TERMINAL_TEXT_ID = "action.Terminal.OpenInTerminal.text"
        const val OPEN_IN_TERMINAL_REWORKED_TEXT_ID = "action.openInTerminal.reworked.text"
        const val OPEN_IN_TERMINAL_CLASSIC_TEXT_ID = "action.openInTerminal.classic.text"
        const val TOOL_WINDOW_ID = "Terminal"
    }
}