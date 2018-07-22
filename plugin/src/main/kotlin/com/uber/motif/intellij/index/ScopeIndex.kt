package com.uber.motif.intellij.index

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.source.JavaFileElementType
import com.intellij.psi.search.ProjectScopeBuilder
import com.intellij.util.indexing.*
import com.intellij.util.io.BooleanDataDescriptor
import com.uber.motif.intellij.psi.isMaybeScopeFile
import java.util.*

/**
 * Invoked during IntelliJ's indexing phase. The DataIndexer marks which files contain a Motif Scope. Once indexed,
 * other parts of the plugin can retrieve all Scope files via ProjectScopeIndex.
 */
class ScopeIndex : ScalarIndexExtension<Boolean>(), PsiDependentIndex {

    private val listeners: MutableSet<() -> Unit> = mutableSetOf()
    private val modificationStamps: MutableMap<String, Long> = mutableMapOf()

    override fun getIndexer() = DataIndexer<Boolean, Void?, FileContent> { fileContent ->
        val isScopeFile = fileContent.psiFile.isMaybeScopeFile()
        putModificationStamp(fileContent.file.name, fileContent.file.modificationStamp)
        notifyListeners()
        mapOf(isScopeFile to null)
    }

    override fun getInputFilter() = object : DefaultFileTypeSpecificInputFilter(JavaFileType.INSTANCE) {

        override fun acceptInput(file: VirtualFile): Boolean {
            // All of the source code in the project. Would need to process more if we want to support Scopes provided
            // as a jar dependency.
            return JavaFileElementType.isInSourceContent(file)
        }
    }

    override fun getVersion() = Random().nextInt()
    override fun getName() = ID
    override fun dependsOnFileContent() = true
    override fun getKeyDescriptor() = BooleanDataDescriptor.INSTANCE!!

    @Synchronized
    fun registerListener(update: () -> Unit) {
        listeners.add(update)
        update()
    }

    @Synchronized
    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    @Synchronized
    private fun putModificationStamp(name: String, timestamp: Long): Boolean {
        return timestamp != modificationStamps.put(name, timestamp)
    }

    /**
     * Returns a superset of all files that contain a Motif Scope.
     */
    fun processScopeFileSuperset(project: Project, processor: (VirtualFile) -> Unit) {
        val projectScopeBuilder = ProjectScopeBuilder.getInstance(project)
        FileBasedIndex.getInstance().getFilesWithKey(ScopeIndex.ID, setOf(true), {
            processor(it)
            true
        }, projectScopeBuilder.buildProjectScope())
    }

    fun refreshFile(project: Project, file: VirtualFile) {
        if (!putModificationStamp(file.name, file.modificationStamp)) {
            return
        }

        val projectScopeBuilder = ProjectScopeBuilder.getInstance(project)
        // processValues forces the file to be reindexed
        FileBasedIndex.getInstance().processValues(
                ScopeIndex.ID,
                true,
                file,
                { _, _ -> true },
                projectScopeBuilder.buildProjectScope())
    }

    companion object {

        private val ID: ID<Boolean, Void> = com.intellij.util.indexing.ID.create("ScopeIndex")

        fun getInstance(): ScopeIndex {
            return FileBasedIndexExtension.EXTENSION_POINT_NAME.findExtension(ScopeIndex::class.java)
                    ?: throw IllegalStateException("Could not find ScopeIndex. Make sure it is registered as a " +
                            "<fileBasedIndex> extension.")
        }
    }
}