package com.terminaltabtailor.settings

import com.terminaltabtailor.enum.TabNameOriginEnum
import com.terminaltabtailor.enum.TabNameSortEnum
import com.terminaltabtailor.enum.TabNameTypeEnum

class TerminalTabTailorSettings {
    var useCurrentDate: Boolean = true
    var performManualRenaming: Boolean = false
    var alreadyExists: Boolean = true

    /** How many parent folders to prepend: 0 gives `child`, 1 `parent/child`, 2 `grandparent/parent/child`. */
    var currentDirectoryParents: Int = 1
    var selectedTabTypeName: TabNameTypeEnum = TabNameTypeEnum.FIRST_DIR_NAME
    var selectedTabTypeSort: TabNameSortEnum = TabNameSortEnum.ASC
    var selectedTabOrigin: TabNameOriginEnum = TabNameOriginEnum.MIXED
    var dateTemplate: String = "dd-MM-yy"
}
