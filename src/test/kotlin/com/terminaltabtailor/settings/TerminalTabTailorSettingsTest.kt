package com.terminaltabtailor.settings

import com.terminaltabtailor.enum.TabNameOriginEnum
import com.terminaltabtailor.enum.TabNameSortEnum
import com.terminaltabtailor.enum.TabNameTypeEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pins the out-of-the-box settings: what a first-time user gets before touching anything.
 * These values are published in the defaults table of README.md, so the two must stay in step.
 */
class TerminalTabTailorSettingsTest {

    private val defaults = TerminalTabTailorSettings()

    @Test
    fun `reuses an existing tab when the names match`() {
        assertTrue(defaults.alreadyExists)
    }

    @Test
    fun `does not prompt the renaming dialogue on every new tab`() {
        assertFalse(defaults.performManualRenaming)
    }

    @Test
    fun `adds the current date to tab names`() {
        assertTrue(defaults.useCurrentDate)
    }

    @Test
    fun `formats the date as dd-MM-yy`() {
        assertEquals("dd-MM-yy", defaults.dateTemplate)
    }

    @Test
    fun `sorts tabs alphabetically`() {
        assertEquals(TabNameSortEnum.ASC, defaults.selectedTabTypeSort)
    }

    @Test
    fun `names tabs after the parent directory`() {
        assertEquals(TabNameTypeEnum.FIRST_DIR_NAME, defaults.selectedTabTypeName)
    }

    @Test
    fun `follows the most recent selection for the TTT button`() {
        assertEquals(TabNameOriginEnum.MIXED, defaults.selectedTabOrigin)
    }
}
