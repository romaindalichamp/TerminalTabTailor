package com.terminaltabtailor.settings

import com.terminaltabtailor.enums.TabNameSort
import com.terminaltabtailor.enums.TabNameType

class TerminalTabTailorSettings {
    var useCurrentDate: Boolean = true
    var performManualRenaming: Boolean = false
    var selectedTabTypeName: TabNameType = TabNameType.FIRST_DIR_NAME
    var selectedTabTypeSort: TabNameSort = TabNameSort.ASC
    var dateTemplate: String = "dd-MM-yy"
}
