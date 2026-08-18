package com.terminaltabtailor.service

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import com.intellij.terminal.frontend.toolwindow.findTabByContent
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.terminaltabtailor.enum.TabNameTypeEnum
import com.terminaltabtailor.settings.TerminalTabTailorSettings
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.TerminalTabsUtil
import com.terminaltabtailor.util.VirtualSelectionUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.util.*

/**
 * Recomputes every terminal tab's name from the directory the shell is currently sitting in, so a
 * `cd` is reflected in the tab without any action from the user. Off by default, enabled by
 * [com.terminaltabtailor.settings.TerminalTabTailorSettings.followCurrentDirectory].
 *
 * It recomputes the name the configured naming style asks for, it does not impose one of its own —
 * see [TerminalTabsUtil.followedName]. A style that cannot be derived from a directory keeps the name
 * the tab opened with, which is also what keeps such a tab reusable: the name it carries stays the one
 * [com.terminaltabtailor.manager.TerminalTabNamesManager.constructNewTabName] will look for.
 *
 * Polled rather than event-driven on purpose: neither engine exposes a public flow of the working
 * directory. `TerminalView.currentDirectory` and `TerminalWidget.getCurrentDirectory()` are plain
 * getters, and the only change callbacks (`TerminalWorkingDirectoryTracker`,
 * `TerminalSessionModel.terminalState`) live behind backend/impl classes this plugin would have to
 * cast to. The interval is coarse enough that reading a handful of tabs costs nothing.
 *
 * Two public sources exist: shell integration, through the reworked engine, and an OS query about the
 * spawned process, through a widget. On Windows the pair only ever answers for PowerShell and cmd —
 * see [TerminalTabsUtil.canFollowWorkingDirectory] — so every other shell is left alone rather than
 * named after a folder it left long ago. The terminal backend does keep a directory of its own, with
 * a heuristic fallback for exactly that case (`TerminalTabsManager.getTerminalTabs()` ->
 * `TerminalSessionTab.workingDirectory`), but the class is Kotlin-`internal` and closed to plugins.
 */
@Service(Service.Level.PROJECT)
class CurrentDirectoryTabNameTracker(
    private val project: Project,
    private val scope: CoroutineScope,
) {

    /**
     * The name this tracker last wrote on each tab. A tab whose display name no longer matches was
     * renamed by someone else — the rename dialogue, most likely — so it is dropped from tracking
     * and keeps the name the user chose. Weak keys let closed tabs fall out on their own.
     */
    private val namesApplied = WeakHashMap<Content, String>()
    private val renamedByUser = Collections.newSetFromMap(WeakHashMap<Content, Boolean>())

    /** What was last logged about each tab, so the poll reports a change rather than every tick. */
    private val diagnostics = WeakHashMap<Content, String>()

    companion object {
        private val LOG = logger<CurrentDirectoryTabNameTracker>()

        private val settingsService by lazy {
            service<TerminalTabTailorSettingsService>()
        }

        /*
         * Fast enough that the tab has caught up by the time the eye moves back to it, slow enough
         * that an idle project is not walking its terminal tabs several times a second.
         */
        private const val POLL_INTERVAL_MS = 500L

        fun getInstance(project: Project): CurrentDirectoryTabNameTracker = project.service()
    }

    fun start() {
        scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)

                if (!settingsService.state.followCurrentDirectory) continue

                try {
                    refreshTabNames()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // A single bad tick must not end the loop for the rest of the project's life.
                    LOG.warn("Could not name the terminal tabs after their working directory", e)
                }
            }
        }
    }

    private suspend fun refreshTabNames() {
        val contentManager = withContext(Dispatchers.EDT) {
            TerminalTabsUtil.getTerminalToolWindow(project)?.contentManager
        } ?: return

        // Reading a Content is as much an EDT matter as writing it.
        val contents = withContext(Dispatchers.EDT) { contentManager.contents.toList() }

        val directories = resolveDirectories(contents)

        withContext(Dispatchers.EDT) {
            applyNames(contentManager, contents, directories)
        }
    }

    private suspend fun resolveDirectories(contents: List<Content>): Map<Content, String?> {
        /*
         * Deliberately off the EDT: the classic engine answers getCurrentDirectory() by blocking on an
         * OS-level query of the shell process, with a 2 second timeout (TerminalWorkingDirectoryManager
         * -> ProcessInfoUtil). Polling that on the EDT freezes the IDE.
         */
        val resolved = withContext(Dispatchers.IO) {
            contents.associateWith(::resolveFromEngine)
        }

        resolved.forEach { (content, directory) -> logOnChange(content, directory.diagnostic) }

        return resolved.mapValues { (_, directory) -> directory.directory }
    }

    private fun applyNames(
        contentManager: ContentManager,
        contents: List<Content>,
        directories: Map<Content, String?>,
    ) {
        val settings = settingsService.state
        var anyRenamed = false

        for (content in contents) {
            if (content in renamedByUser) continue

            val nameApplied = namesApplied[content]
            if (nameApplied != null && nameApplied != content.displayName) {
                renamedByUser.add(content)
                namesApplied.remove(content)
                continue
            }

            /*
             * Nothing to follow: either the shell reports no directory - resolveFromEngine has already
             * logged why - or the naming style is not a function of one. Either way the tab keeps the
             * name it was opened under.
             */
            val followedName = directories[content]
                ?.let { followedNameFor(it, settings) }
                ?: continue

            /*
             * Numbering is resolved against the *other* tabs so that a tab keeps its own name instead
             * of counting itself as a collision — and drops the suffix again once the tab it clashed
             * with moves away or closes.
             */
            val newDisplayName = TerminalTabsUtil.incrementNumberInName(
                contents.filterNot { it === content },
                TerminalTabsUtil.appendDate(followedName, settings)
            )

            if (newDisplayName != content.displayName) {
                applyName(content, newDisplayName)
                anyRenamed = true
            }
            namesApplied[content] = newDisplayName
        }

        if (anyRenamed) {
            TerminalTabsUtil.sortTabs(contentManager, settingsService)
        }
    }

    /*
     * The tab's own naming style decides what following the shell means: a directory style is named
     * after the directory itself, a module style after the module owning it, and the styles that name
     * something a directory cannot yield answer null so their tabs are left alone.
     */
    private fun followedNameFor(currentDirectory: String, settings: TerminalTabTailorSettings): String? {
        val module = moduleOwning(currentDirectory, settings.selectedTabTypeName)

        return TerminalTabsUtil.followedName(
            settings.selectedTabTypeName,
            currentDirectory,
            settings.currentDirectoryParents,
            module?.name,
            module?.let { VirtualSelectionUtil.getModuleDirectoryPath(it) },
        )
    }

    /*
     * Only the two module styles pay for this: mapping a shell's directory back to a module costs a
     * VFS lookup and an index query, per tab, per tick.
     *
     * findFileByPath rather than refreshAndFindFileByPath, which would hit the disk from the EDT. A
     * directory the VFS has never loaded - or one that is not local at all, such as the path a WSL
     * shell reports - resolves to no module, and the tab keeps the name it has. So does a shell that
     * has walked out of the project entirely.
     *
     * The path is normalised first: the VFS keys on '/' whatever the host, while a Windows shell
     * reports 'C:\...' through both sources.
     */
    private fun moduleOwning(currentDirectory: String, nameType: TabNameTypeEnum): Module? {
        if (nameType != TabNameTypeEnum.MODULE_NAME && nameType != TabNameTypeEnum.MODULE_DIR_NAME) {
            return null
        }

        val directory = LocalFileSystem.getInstance()
            .findFileByPath(FileUtil.toSystemIndependentName(currentDirectory))
            ?: return null

        return runReadAction { ProjectFileIndex.getInstance(project).getModuleForFile(directory) }
    }

    private fun applyName(content: Content, newDisplayName: String) {
        /*
         * Same safety net as a freshly created tab: userDefinedTitle outranks every other component
         * of TerminalTitle.buildTitle(), so the shell cannot report its own title over ours.
         */
        TerminalToolWindowManager
            .findWidgetByContent(content)
            ?.terminalTitle
            ?.change { userDefinedTitle = newDisplayName }

        content.displayName = newDisplayName
        content.tabName = newDisplayName
    }

    /*
     * Logged at info, because whether a shell can be followed at all depends on the engine and on
     * shell integration, neither of which the user can see - and a tab that quietly keeps its name
     * gives them nothing to go on. Only on change, so it stays one line per tab.
     */
    private fun logOnChange(content: Content, diagnostic: String) {
        if (diagnostics.put(content, diagnostic) != diagnostic) {
            LOG.info("Following terminal tab: $diagnostic")
        }
    }

    private fun resolveFromEngine(content: Content): ResolvedDirectory {
        val reworkedTab = TerminalToolWindowTabsManager.getInstance(project).findTabByContent(content)
        val engine = if (reworkedTab != null) "reworked tab" else "classic tab"

        /*
         * Shell integration first, and on its own terms: it is the shell's own account of where it
         * stands, so it is right whatever the shell and needs no vetting. It is also the *only* source
         * a reworked tab has — those expose no widget — so asking for one before this would leave
         * every reworked tab unresolved.
         */
        val reportedDirectory = reworkedTab?.view?.getCurrentDirectory()
        if (reportedDirectory != null) {
            return ResolvedDirectory(
                reportedDirectory,
                "$engine, shell integration reports '$reportedDirectory'"
            )
        }

        val widget = TerminalToolWindowManager.findWidgetByContent(content)
            ?: return ResolvedDirectory(
                null,
                "$engine with no shell integration and no widget - a session still starting up"
            )

        val shellCommand = widget.shellCommand

        /*
         * Falling back to the OS, which only knows the directory of the process it spawned. That
         * stands still for every shell but the few canFollowWorkingDirectory admits.
         */
        if (!TerminalTabsUtil.canFollowWorkingDirectory(shellCommand)) {
            return ResolvedDirectory(
                null,
                "$engine running ${shellCommand ?: "an unreported shell"} - not a shell whose " +
                        "working directory can be followed, so the tab keeps the name it opened with"
            )
        }

        val directory = widget.getCurrentDirectory()

        return ResolvedDirectory(
            directory,
            "$engine running $shellCommand, no shell integration, the OS reports '$directory'"
        )
    }

    private data class ResolvedDirectory(val directory: String?, val diagnostic: String)
}
