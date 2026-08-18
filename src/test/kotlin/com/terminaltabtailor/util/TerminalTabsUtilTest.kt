package com.terminaltabtailor.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.terminaltabtailor.enum.TabNameSortEnum
import com.terminaltabtailor.settings.TerminalTabTailorSettings
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.InOrder
import org.mockito.Mockito.*

class TerminalTabsUtilTest {

    @Test
    fun `test getLastOpenedTab returns last tab when content exists`() {
        val contentManager = mock(ContentManager::class.java)
        val lastContent = mock(Content::class.java)

        `when`(contentManager.contentCount).thenReturn(5)
        `when`(contentManager.getContent(4)).thenReturn(lastContent)

        val result = TerminalTabsUtil.getLastOpenedTab(contentManager)

        assertEquals(lastContent, result, "The last opened tab should be returned")
    }

    @Test
    fun `test getLastOpenedTab returns null when no content exists`() {
        val contentManager = mock(ContentManager::class.java)

        `when`(contentManager.contentCount).thenReturn(0)

        val result = TerminalTabsUtil.getLastOpenedTab(contentManager)

        assertNull(result, "No tab should be returned when no content exists")
    }

    @Test
    fun `getNextAvailableTabName returns prefix with next available number`() {
        val prefix = "Tab"
        val existingTabs = listOf(
            createMockContent("Tab (1)"),
            createMockContent("Tab (2)"),
            createMockContent("Tab (3)")
        )

        val result = TerminalTabsUtil.incrementNumberInName(existingTabs, prefix)

        assertEquals("Tab (4)", result)
    }

    @Test
    fun `getNextAvailableTabName returns no prefix only when no tab exists`() {
        val prefix = "Tab"
        val existingTabs = listOf<Content>()

        val result = TerminalTabsUtil.incrementNumberInName(existingTabs, prefix)

        assertEquals("Tab", result)
    }

    @Test
    fun `getNextAvailableTabName returns prefix only when no numbers are used`() {
        val prefix = "Tab"
        val existingTabs = listOf(createMockContent("Tab"))

        val result = TerminalTabsUtil.incrementNumberInName(existingTabs, prefix)

        assertEquals("Tab (2)", result)
    }


    @Test
    fun `getNextAvailableTabName skips missing numbers`() {
        val prefix = "Tab"
        val existingTabs = listOf(
            createMockContent("Tab"),
            createMockContent("Tab (3)")
        )

        val result = TerminalTabsUtil.incrementNumberInName(existingTabs, prefix)

        assertEquals("Tab (2)", result)
    }


    @Test
    fun `getNextAvailableTabName treats a dated tab as occupying the undated name`() {
        val existingTabs = listOf(createMockContent("main <03-04-24>"))

        val result = TerminalTabsUtil.incrementNumberInName(existingTabs, "main")

        assertEquals(
            "main (2)", result,
            "Names are matched by prefix, so turning the date option off collides with dated tabs"
        )
    }

    @Test
    fun `getNextAvailableTabName treats a longer name sharing the prefix as a collision`() {
        val existingTabs = listOf(createMockContent("srcx"))

        val result = TerminalTabsUtil.incrementNumberInName(existingTabs, "src")

        assertEquals(
            "src (2)", result,
            "Prefix matching is not bounded to a word, so 'srcx' counts against 'src'"
        )
    }

    @Test
    fun `alreadyExistingTerminalTab finds the tab whose name matches`() {
        val wanted = createMockContent("controllers <03-04-24>")
        val tabs = listOf(createMockContent("resources <03-04-24>"), wanted)

        val result = TerminalTabsUtil.alreadyExistingTerminalTab(tabs, "controllers <03-04-24>")

        assertEquals(wanted, result)
    }

    @Test
    fun `alreadyExistingTerminalTab returns null when nothing matches`() {
        val tabs = listOf(createMockContent("resources <03-04-24>"))

        val result = TerminalTabsUtil.alreadyExistingTerminalTab(tabs, "controllers <03-04-24>")

        assertNull(result, "A new tab must be opened when no name matches")
    }

    @Test
    fun `alreadyExistingTerminalTab requires an exact name, not a prefix`() {
        val tabs = listOf(createMockContent("controllers <03-04-24> (2)"))

        val result = TerminalTabsUtil.alreadyExistingTerminalTab(tabs, "controllers <03-04-24>")

        assertNull(result, "Reuse matches the whole name, unlike the numbering helper")
    }

    @Test
    fun `selectNewTab selects the tab carrying the given name`() {
        val contentManager = mock(ContentManager::class.java)
        val wanted = createMockContent("controllers")
        val other = createMockContent("resources")

        `when`(contentManager.contents).thenReturn(arrayOf(other, wanted))

        TerminalTabsUtil.selectNewTab(contentManager, "controllers")

        verify(contentManager).setSelectedContent(wanted)
        verify(contentManager, never()).setSelectedContent(other)
    }

    @Test
    fun `selectNewTab selects nothing when no tab matches`() {
        val contentManager = mock(ContentManager::class.java)
        // Built before the stubbing below: createMockContent stubs its own mock, and nesting that
        // inside thenReturn() leaves the outer stubbing unfinished.
        val contents = arrayOf(createMockContent("resources"))

        `when`(contentManager.contents).thenReturn(contents)

        TerminalTabsUtil.selectNewTab(contentManager, "controllers")

        verify(contentManager, never()).setSelectedContent(any())
    }

    @Test
    fun `selectNewTab leaves the last duplicate selected`() {
        val contentManager = mock(ContentManager::class.java)
        val first = createMockContent("controllers")
        val second = createMockContent("controllers")

        `when`(contentManager.contents).thenReturn(arrayOf(first, second))

        TerminalTabsUtil.selectNewTab(contentManager, "controllers")

        val inOrder = inOrder(contentManager)
        inOrder.verify(contentManager).setSelectedContent(first)
        inOrder.verify(contentManager).setSelectedContent(second)
    }

    /**
     * Sorting is a remove-then-re-add of every tab, and removing the selected one hands the selection
     * to a neighbour. A tab that renames itself after the shell's working directory sorts the strip
     * without the user asking, so losing the selection there would move them mid-keystroke.
     */
    @Test
    fun `sortTabs leaves the selected tab selected`() {
        val contentManager = mock(ContentManager::class.java)
        val settingsState = mock(TerminalTabTailorSettings::class.java)
        val settingsService = mock(TerminalTabTailorSettingsService::class.java)
        val contents = arrayOf(createMockContent("Zeta"), createMockContent("Alpha"))
        val selected = contents[0]

        `when`(contentManager.contents).thenReturn(contents)
        `when`(contentManager.selectedContent).thenReturn(selected)
        `when`(settingsService.state).thenReturn(settingsState)
        `when`(settingsState.selectedTabTypeSort).thenReturn(TabNameSortEnum.ASC)

        TerminalTabsUtil.sortTabs(contentManager, settingsService) { false }

        val inOrder = inOrder(contentManager)
        inOrder.verify(contentManager).addContent(contents[1])
        inOrder.verify(contentManager).addContent(selected)
        inOrder.verify(contentManager).setSelectedContent(selected, false)
    }

    /**
     * Re-selecting alone leaves keyboard focus outside the terminal, so the user has to click back
     * into the command line before they can carry on typing.
     */
    @Test
    fun `sortTabs hands focus back to a terminal that had it`() {
        val contentManager = mock(ContentManager::class.java)
        val settingsState = mock(TerminalTabTailorSettings::class.java)
        val settingsService = mock(TerminalTabTailorSettingsService::class.java)
        val contents = arrayOf(createMockContent("Zeta"), createMockContent("Alpha"))
        val selected = contents[0]

        `when`(contentManager.contents).thenReturn(contents)
        `when`(contentManager.selectedContent).thenReturn(selected)
        `when`(settingsService.state).thenReturn(settingsState)
        `when`(settingsState.selectedTabTypeSort).thenReturn(TabNameSortEnum.ASC)

        TerminalTabsUtil.sortTabs(contentManager, settingsService) { it === selected }

        verify(contentManager).setSelectedContent(selected, true)
    }

    /**
     * A tab renaming itself after the shell's working directory sorts the strip without anyone asking
     * for it, so grabbing focus would yank a user typing in the editor away from their code.
     */
    @Test
    fun `sortTabs does not steal focus from elsewhere in the IDE`() {
        val contentManager = mock(ContentManager::class.java)
        val settingsState = mock(TerminalTabTailorSettings::class.java)
        val settingsService = mock(TerminalTabTailorSettingsService::class.java)
        val contents = arrayOf(createMockContent("Zeta"), createMockContent("Alpha"))
        val selected = contents[0]

        `when`(contentManager.contents).thenReturn(contents)
        `when`(contentManager.selectedContent).thenReturn(selected)
        `when`(settingsService.state).thenReturn(settingsState)
        `when`(settingsState.selectedTabTypeSort).thenReturn(TabNameSortEnum.ASC)

        TerminalTabsUtil.sortTabs(contentManager, settingsService) { false }

        verify(contentManager).setSelectedContent(selected, false)
        verify(contentManager, never()).setSelectedContent(selected, true)
    }

    /**
     * Reordering removes every tab before re-adding it, which empties the ContentManager outright when
     * a single tab is open — and the terminal tool window collapses as soon as its last content goes.
     */
    @Test
    fun `sortTabs leaves a strip that is already in order untouched`() {
        val contentManager = mock(ContentManager::class.java)
        val settingsState = mock(TerminalTabTailorSettings::class.java)
        val settingsService = mock(TerminalTabTailorSettingsService::class.java)
        val contents = arrayOf(createMockContent("Alpha"), createMockContent("Zeta"))

        `when`(contentManager.contents).thenReturn(contents)
        `when`(settingsService.state).thenReturn(settingsState)
        `when`(settingsState.selectedTabTypeSort).thenReturn(TabNameSortEnum.ASC)

        TerminalTabsUtil.sortTabs(contentManager, settingsService)

        verify(contentManager, never()).removeContent(any(), anyBoolean())
        verify(contentManager, never()).addContent(any())
    }

    @Test
    fun `sortTabs leaves a lone tab alone`() {
        val contentManager = mock(ContentManager::class.java)
        val settingsState = mock(TerminalTabTailorSettings::class.java)
        val settingsService = mock(TerminalTabTailorSettingsService::class.java)
        val contents = arrayOf(createMockContent("wrapper"))

        `when`(contentManager.contents).thenReturn(contents)
        `when`(settingsService.state).thenReturn(settingsState)
        `when`(settingsState.selectedTabTypeSort).thenReturn(TabNameSortEnum.ASC)

        TerminalTabsUtil.sortTabs(contentManager, settingsService)

        verify(contentManager, never()).removeContent(any(), anyBoolean())
    }

    @Test
    fun `sortTabs selects nothing when no tab was selected`() {
        val contentManager = mock(ContentManager::class.java)
        val settingsState = mock(TerminalTabTailorSettings::class.java)
        val settingsService = mock(TerminalTabTailorSettingsService::class.java)
        val contents = arrayOf(createMockContent("Zeta"), createMockContent("Alpha"))

        `when`(contentManager.contents).thenReturn(contents)
        `when`(contentManager.selectedContent).thenReturn(null)
        `when`(settingsService.state).thenReturn(settingsState)
        `when`(settingsState.selectedTabTypeSort).thenReturn(TabNameSortEnum.ASC)

        TerminalTabsUtil.sortTabs(contentManager, settingsService) { false }

        verify(contentManager, never()).setSelectedContent(any(), anyBoolean())
    }

    @Test
    fun `ascSort should order contents alphabetically`() {
        val contentManager = mock(ContentManager::class.java)
        val content1 = mock(Content::class.java)
        val content2 = mock(Content::class.java)
        val settingsState = mock(TerminalTabTailorSettings::class.java)
        val settingsService = mock(TerminalTabTailorSettingsService::class.java)

        `when`(content1.displayName).thenReturn("Zeta")
        `when`(content2.displayName).thenReturn("Alpha")
        `when`(contentManager.contents).thenReturn(arrayOf(content1, content2))
        `when`(settingsService.state).thenReturn(settingsState)
        `when`(settingsState.selectedTabTypeSort).thenReturn(TabNameSortEnum.ASC)

        TerminalTabsUtil.sortTabs(contentManager, settingsService)

        val inOrder = inOrder(contentManager)
        inOrder.verify(contentManager).removeContent(eq(content2), eq(false)) // Alpha
        inOrder.verify(contentManager).addContent(eq(content2))
        inOrder.verify(contentManager).removeContent(eq(content1), eq(false)) // Zeta
        inOrder.verify(contentManager).addContent(eq(content1))
    }

    @Test
    fun `test descDateSort sorts correctly with various date formats and cases`() {
        val project = mock(Project::class.java)
        val toolWindowManager = mock(ToolWindowManager::class.java)
        val toolWindow = mock(ToolWindow::class.java)
        val contentManager = mock(ContentManager::class.java)
        val settingsState = mock(TerminalTabTailorSettings::class.java)
        val settingsService = mock(TerminalTabTailorSettingsService::class.java)

        val contents = arrayOf(
            createMockContent("UPPERCASE <03-04-24>"),
            createMockContent("nodate"),
            createMockContent("WRONG_DATE <2222-55-888>"),
            createMockContent("aaaa <02-04-24>")
        )

        `when`(project.getService(ToolWindowManager::class.java)).thenReturn(toolWindowManager)
        `when`(toolWindowManager.getToolWindow("Terminal")).thenReturn(toolWindow)
        `when`(toolWindow.contentManager).thenReturn(contentManager)
        `when`(contentManager.contents).thenReturn(contents)
        `when`(settingsService.state).thenReturn(settingsState)
        `when`(settingsState.selectedTabTypeSort).thenReturn(TabNameSortEnum.DESC_DATE)
        `when`(settingsState.dateTemplate).thenReturn("dd-MM-yy")

        TerminalTabsUtil.sortTabs(contentManager, settingsService)

        val inOrder: InOrder = inOrder(contentManager)
        inOrder.verify(contentManager).addContent(contents[0])
        inOrder.verify(contentManager).addContent(contents[3])
        inOrder.verify(contentManager).addContent(contents[1])
        inOrder.verify(contentManager).addContent(contents[2])
    }

    @Test
    fun `test doNotSort sort`() {
        val project = mock(Project::class.java)
        val toolWindowManager = mock(ToolWindowManager::class.java)
        val toolWindow = mock(ToolWindow::class.java)
        val contentManager = mock(ContentManager::class.java)
        val settingsState = mock(TerminalTabTailorSettings::class.java)
        val settingsService = mock(TerminalTabTailorSettingsService::class.java)

        val contents = arrayOf(
            createMockContent("UPPERCASE <03-04-24>"),
            createMockContent("nodate"),
            createMockContent("WRONG_DATE <2222-55-888>"),
            createMockContent("aaaa <02-04-24>")
        )

        `when`(project.getService(ToolWindowManager::class.java)).thenReturn(toolWindowManager)
        `when`(toolWindowManager.getToolWindow("Terminal")).thenReturn(toolWindow)
        `when`(toolWindow.contentManager).thenReturn(contentManager)
        `when`(contentManager.contents).thenReturn(contents)
        `when`(settingsService.state).thenReturn(settingsState)
        `when`(settingsState.selectedTabTypeSort).thenReturn(TabNameSortEnum.NO_SORT)
        `when`(settingsState.dateTemplate).thenReturn("dd-MM-yy")

        TerminalTabsUtil.sortTabs(contentManager, settingsService)

        verifyNoInteractions(contentManager)
    }

    private fun createMockContent(name: String): Content {
        val content = mock(Content::class.java)
        `when`(content.tabName).thenReturn(name)
        `when`(content.displayName).thenReturn(name)
        return content
    }

}