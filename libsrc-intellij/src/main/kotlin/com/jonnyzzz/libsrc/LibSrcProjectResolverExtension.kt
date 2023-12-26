package com.jonnyzzz.libsrc

import org.gradle.api.Project
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.Serializable
import kotlin.reflect.KProperty

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


    override fun buildAll(modelName: String?, project: Project?): Any? {
        println("Hello from libsrc Import Service")
        val ext = project?.dependencies?.extensions?.findByName("libsrc") ?: return null

        try {
            val configurations: Map<*, *> by ext
            val items: List<Any> = configurations.values.filterNotNull()
            println("Hello from libsrc Import Service: $configurations")

            for (item in items) {
                val configurationName: String by item
                val targets: List<*> by item
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
                    override val libraryName: String = "test"
                    override val sourceFolders: List<String> = listOf("test2")
                }
            )
        }
    }
}

private fun String.cap() = replaceFirstChar { it.uppercaseChar() }

private inline operator fun <reified R> Any.getValue(nothing: Nothing?, property: KProperty<*>): R {
    val fieldName = property.name
    val methodName = "get" + property.name.cap()

    var clazz = this.javaClass
    while (clazz != Any::class.java) {
        try {
            val method = clazz.getDeclaredMethod(methodName)
            method.isAccessible = true
            return method.invoke(this) as R
        } catch (t: NoSuchMethodException) {
            //OK
        }

        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            return field.get(this) as R
        } catch (t: NoSuchFieldException) {
            //OK
        }

        clazz = clazz.superclass
    }
    throw NoSuchMethodException("Failed to find method $methodName or field $fieldName on ${this.javaClass.name}")
}
