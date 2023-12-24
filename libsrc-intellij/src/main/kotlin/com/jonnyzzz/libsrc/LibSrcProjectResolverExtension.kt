package com.jonnyzzz.libsrc

import org.gradle.api.Project
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.Serializable

interface LibSrcProjectInfo : Serializable {
    val infos: List<LibSrcProjectInfoItem>
}

interface LibSrcProjectInfoItem : Serializable {
    val libraryName: String
    val sourceFolders: List<String>
}

class LibSrcProjectResolverExtension : AbstractProjectResolverExtension() {

    override fun getExtraProjectModelClasses(): Set<Class<*>> {
        return setOf(LibSrcProjectInfoItem::class.java, LibSrcProjectInfo::class.java)
    }

    override fun getToolingExtensionsClasses(): Set<Class<*>> {
        return setOf(LibSrcProjectModelBuilderService::class.java)
    }
}

class LibSrcProjectModelBuilderService : ModelBuilderService {
    override fun canBuild(modelName: String?): Boolean {
        return modelName == LibSrcProjectInfoItem::class.java.name
    }

    override fun buildAll(modelName: String?, project: Project?): Any {
        println("Hello from libsrc Import Service")
        return object : LibSrcProjectInfo {
            override val infos: List<LibSrcProjectInfoItem> = listOf(
                object : LibSrcProjectInfoItem {
                    override val libraryName: String = "test"
                    override val sourceFolders: List<String> = listOf("test2")
                }
            )
        }
    }
}
