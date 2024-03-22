package com.terminaltabtailor.settings

import com.terminaltabtailor.enum.TabNameOriginEnum
import com.terminaltabtailor.enum.TabNameSortEnum
import com.terminaltabtailor.enum.TabNameTypeEnum

class TerminalTabTailorSettings {
    var useCurrentDate: Boolean = true
    var performManualRenaming: Boolean = false
    var alreadyExists: Boolean = false
    var selectedTabTypeName: TabNameTypeEnum = TabNameTypeEnum.FIRST_DIR_NAME
    var selectedTabTypeSort: TabNameSortEnum = TabNameSortEnum.ASC
    var selectedTabOrigin: TabNameOriginEnum = TabNameOriginEnum.MIXED
    var dateTemplate: String = "dd-MM-yy"
}
