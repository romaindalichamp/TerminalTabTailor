package com.terminaltabtailor.settings

import com.terminaltabtailor.enums.TabNameType

class TerminalTabTailorSettings {
    var useCurrentDate: Boolean = true
    var ascSort: Boolean = false
    var descDateSort: Boolean = true
    var performManualRenaming: Boolean = false
    var selectedTabTypeName: TabNameType = TabNameType.FIRST_DIR_NAME
    var dateTemplate: String = "dd-MM-yy"
}
