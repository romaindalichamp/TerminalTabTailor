package com.terminaltabtailor.dto

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile

class VirtualSelection {
    var lastSelectedVirtualFile: VirtualFile? = null
    var lastSelectedVirtualFileParent: VirtualFile? = null
    var lastSelectedVirtualFileParentModule: Module? = null
    var lastSelectedVirtualFileParentModuleDirName: String? = null

}