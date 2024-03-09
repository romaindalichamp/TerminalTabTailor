package com.terminaltabtailor.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.InOrder
import org.mockito.Mockito
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

        val result = TerminalTabsUtil.getNextAvailableTabName(existingTabs, prefix)

        assertEquals(Pair("Tab (4)", "Tab (4)"), result)
    }

    @Test
    fun `getNextAvailableTabName returns prefix only when no numbers are used`() {
        val prefix = "Tab"
        val existingTabs = listOf<Content>()

        val result = TerminalTabsUtil.getNextAvailableTabName(existingTabs, prefix)

        assertEquals(Pair("Tab", "Tab (1)"), result)
    }

    @Test
    fun `getNextAvailableTabName skips missing numbers`() {
        val prefix = "Tab"
        val existingTabs = listOf(
                createMockContent("Tab (1)"),
                createMockContent("Tab (3)")
        )

        val result = TerminalTabsUtil.getNextAvailableTabName(existingTabs, prefix)

        assertEquals(Pair("Tab (2)", "Tab (2)"), result)
    }

    @Test
    fun `ascSort should order contents alphabetically`() {
        val project = mock(Project::class.java)
        val toolWindowManager = mock(ToolWindowManager::class.java)
        val toolWindow = mock(ToolWindow::class.java)
        val contentManager = mock(ContentManager::class.java)
        val content1 = mock(Content::class.java)
        val content2 = mock(Content::class.java)

        `when`(project.getService(ToolWindowManager::class.java)).thenReturn(toolWindowManager)
        `when`(toolWindowManager.getToolWindow("Terminal")).thenReturn(toolWindow)
        `when`(toolWindow.contentManager).thenReturn(contentManager)
        `when`(content1.displayName).thenReturn("Zeta")
        `when`(content2.displayName).thenReturn("Alpha")
        `when`(contentManager.contents).thenReturn(arrayOf(content1, content2))

        TerminalTabsUtil.ascSort(project)

        val inOrder = inOrder(contentManager)
        inOrder.verify(contentManager).removeContent(eq(content2), eq(false)) // Alpha should come first
        inOrder.verify(contentManager).addContent(eq(content2))
        inOrder.verify(contentManager).removeContent(eq(content1), eq(false)) // Zeta should come second
        inOrder.verify(contentManager).addContent(eq(content1))
    }
    @Test
    fun `test descDateSort sorts correctly with various date formats and cases`() {
        val contentManager = mock(ContentManager::class.java)

        val contents = arrayOf(
            createMockContent("UPPERCASE <03-04-24>"),
            createMockContent("nodate"),
            createMockContent("WRONG_DATE <2222-55-888>"),
            createMockContent("aaaa <02-04-24>")
        )

        `when`(contentManager.contents).thenReturn(contents)

        val sortedContents = TerminalTabsUtil.descDateSort(contentManager,"dd-MM-yy")
        println(sortedContents)

        val inOrder: InOrder = inOrder(contentManager)
        inOrder.verify(contentManager).addContent(contents[0])
        inOrder.verify(contentManager).addContent(contents[3])
        inOrder.verify(contentManager).addContent(contents[1])
        inOrder.verify(contentManager).addContent(contents[2])
    }

    private fun createMockContent(name: String): Content {
        val content = mock(Content::class.java)
        `when`(content.tabName).thenReturn(name)
        `when`(content.displayName).thenReturn(name)
        return content
    }

}