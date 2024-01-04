package com.jonnyzzz.libsrc

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.gradle.api.Project
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.Serializable
import kotlin.reflect.KProperty

interface LibSrcProjectInfo : Serializable {
    val infos: List<LibSrcProjectInfoItem>
}

interface LibSrcProjectInfoItem : Serializable {
    val sourceSetName: String
    val libraryName: String
    val sourceFolders: List<String>
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

        val gradleModules: MutableCollection<DataNode<GradleSourceSetData>> =
            ExternalSystemApiUtil.getChildren(ideModule, GradleSourceSetData.KEY)

        for (libSrcInfo in libSrcInfos.infos) {
            val gradleModule: DataNode<GradleSourceSetData>? =
                gradleModules.find { libSrcInfo.sourceSetName == it.data.moduleName /*private getSourceSetName()*/ }

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
        println("Hello from libsrc Import Service")
        val ext = project?.dependencies?.extensions?.findByName("libsrc") ?: return null

        try {
            val configurations: Map<*, *> by ext.dynamic()
            val items: List<Any> = configurations.values.filterNotNull()
            println("Hello from libsrc Import Service: $configurations")

            for (item in items) {
                val configurationName: String by item.dynamic()
                val targets: List<*> by item.dynamic()
                println("Hello from libsrc: $configurationName -> $targets")
            }
            println("Bye from libsrc Import Service")
        } catch (t: Throwable) {
            println("Crash from libsrc Import Service: " + t.message + "\n\n" + t)
            return null
        }

        return object : LibSrcProjectInfo {
            override val infos: List<LibSrcProjectInfoItem> = listOf(
                object : LibSrcProjectInfoItem {
                    override val sourceSetName: String = "main"
                    override val libraryName: String = "test"
                    override val sourceFolders: List<String> = listOf("test2")
                }
            )
        }
    }
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