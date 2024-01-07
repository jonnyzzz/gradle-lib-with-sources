package com.jonnyzzz.libsrc

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.Serializable
import kotlin.reflect.KProperty

interface LibSrcProjectInfo : Serializable {
    val fetchSourcesTaskName: String
    val infos: List<LibSrcProjectInfoItem>
}

interface LibSrcProjectInfoItem : Serializable {
    val sourceSetName: String
    val sourceFolders: Collection<String>
}

class LibSrcProjectResolverExtension : AbstractProjectResolverExtension() {
    private val logger = thisLogger()

    override fun populateModuleExtraModels(gradleModel: IdeaModule, ideModule: DataNode<ModuleData>) {
        resolverCtx.getExtraProject(gradleModel, LibSrcProjectInfo::class.java)?.let { libSrcInfos ->
            processLibrarySources(libSrcInfos, gradleModel, ideModule)
        }

        super.populateModuleExtraModels(gradleModel, ideModule)
    }

    private fun processLibrarySources(
        libSrcInfos: LibSrcProjectInfo,
        gradleModel: IdeaModule,
        ideModule: DataNode<ModuleData>
    ) {
        logger.warn("Found model $libSrcInfos. $gradleModel, ${ideModule.data}, ${ideModule.children}")

        //resolving sources sources
        val buildLauncher = resolverCtx.connection.newBuild().forTasks(libSrcInfos.fetchSourcesTaskName)
        resolverCtx.cancellationTokenSource?.token()?.let { token -> buildLauncher.withCancellationToken(token) }

        //TODO: no progress is reported back
        buildLauncher.run()

        val gradleModules: MutableCollection<DataNode<GradleSourceSetData>> =
            ExternalSystemApiUtil.getChildren(ideModule, GradleSourceSetData.KEY)

        for (libSrcInfo in libSrcInfos.infos) {
            val gradleModule: DataNode<GradleSourceSetData>? =
                gradleModules.firstOrNull { libSrcInfo.sourceSetName == it.data.moduleName /*private getSourceSetName()*/ }

            if (gradleModule == null) {
                logger.warn("Failed to resolve module $libSrcInfo")
                continue
            }

            val library = LibraryData(GradleConstants.SYSTEM_ID, "sources"/*TODO: Unique ID*/)
            for (src in libSrcInfo.sourceFolders) {
                library.addPath(LibraryPathType.SOURCE, src)
            }
            val data = LibraryDependencyData(gradleModule.data, library, LibraryLevel.MODULE)
            gradleModule.createChild(ProjectKeys.LIBRARY_DEPENDENCY, data)
        }
    }

    override fun getExtraProjectModelClasses(): Set<Class<*>> {
        return setOf(LibSrcProjectInfo::class.java)
    }

    override fun getToolingExtensionsClasses(): Set<Class<*>> {
        return setOf(LibSrcProjectModelBuilderService::class.java)
    }
}

class LibSrcProjectModelBuilderService : ModelBuilderService {
    override fun canBuild(modelName: String?): Boolean {
        return modelName == LibSrcProjectInfo::class.java.name
    }

    override fun buildAll(modelName: String?, project: Project?): Any? {
        if (modelName == null || project == null) return null
        if (!canBuild(modelName)) return null

        val libSrcProjectExtension = project.extensions
            .findByName("libsrc") as? Collection<*> ?: return null

        val srcConfigurationTaskName: String by libSrcProjectExtension.dynamic()

        try {
            val items = mutableListOf<LibSrcProjectInfoItem>()

            for (item in libSrcProjectExtension.filterNotNull().toList()) {
                val configurationName: String by item.dynamic()
                val targets: List<*> by item.dynamic()
                val actualFiles = targets
                    .filterIsInstance<ConfigurableFileTree>()
                    .flatMap { it.files }
                    .map { it.absolutePath }
                    .toSortedSet()

                if (actualFiles.isEmpty()) continue

                val modelItem = LibSrcProjectInfoItemData(
                    //TODO: map configuration to source set decently
                    sourceSetName = when (configurationName) {
                        "implementation" -> "main"
                        "testImplementation" -> "test"
                        else -> configurationName

                    },
                    //TODO: allow not only fileset, but also a directory of sources
                    sourceFolders = actualFiles,
                )

                items += modelItem
            }

            println("Bye from libsrc Import Service")

            return LibSrcProjectInfoData(
                fetchSourcesTaskName = srcConfigurationTaskName,
                infos = items.toList()
            )
        } catch (t: Throwable) {
            println("Crash from libsrc Import Service: " + t.message + "\n\n" + t)
            return null
        }
    }
}

class LibSrcProjectInfoData(
    override val fetchSourcesTaskName: String,
    override val infos: List<LibSrcProjectInfoItem>,
) : LibSrcProjectInfo, Serializable

class LibSrcProjectInfoItemData(
    override val sourceSetName: String,
    override val sourceFolders: Collection<String>,
) : LibSrcProjectInfoItem, Serializable {
    override fun toString(): String = "LibSrcProjectInfoItem{configuration=$sourceSetName, files count: ${sourceFolders.size}"
}

fun Any.dynamic() = ReflectionDelegate(this)

class ReflectionDelegate(val host: Any) {
    inline operator fun <reified R> getValue(nothing: Nothing?, property: KProperty<*>): R {
        val fieldName = property.name
        val methodName = "get" + property.name.replaceFirstChar { it.uppercaseChar() }

        var clazz = host.javaClass
        while (clazz != Any::class.java) {
            try {
                val method = clazz.getDeclaredMethod(methodName)
                method.isAccessible = true
                return method.invoke(host) as R
            } catch (t: NoSuchMethodException) {
                //OK
            }

            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                return field.get(host) as R
            } catch (t: NoSuchFieldException) {
                //OK
            }

            clazz = clazz.superclass
        }
        throw NoSuchMethodException("Failed to find method $methodName or field $fieldName on ${host.javaClass.name}")
    }
}