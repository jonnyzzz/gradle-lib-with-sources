@file:Suppress("unused")

package com.jonnyzzz.libsrc

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

@Suppress("unused")
class LibSrcPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hello from libsrc plugin")

        val libsrcConfig: Configuration = project.configurations.create("libsrc-configuration")
        libsrcConfig.isVisible = false

        val namedDomainObjectSet = project.objects.domainObjectContainer(LibSrcItem::class.java)

        project.configurations.all {
            namedDomainObjectSet.add(LibSrcItem(it.name))
        }

        project.dependencies.extensions.create("libsrc", LibSrcExt::class.java,
            project,
            libsrcConfig,
            namedDomainObjectSet
        )
    }
}

abstract class LibSrcExt(
    private val project: Project,
    @Suppress("MemberVisibilityCanBePrivate")
    val libsrcConfig: Configuration,
    private val objects: NamedDomainObjectContainer<LibSrcItem>,
) : NamedDomainObjectContainer<LibSrcItem> by objects {
    private val configurations = mutableMapOf<String, LibSrcItem>()

    open operator fun invoke(forConfiguration: String, vararg files: Any) {
        return libsrc(forConfiguration, files)
    }

    open fun libsrc(forConfiguration: String, vararg files: Any) {
        val libsrcTask = project.tasks.create("libsrc-$forConfiguration-1") {
            //TODO: make task depend from any of the files (and providers)
        }

        return
        //TODO: alternatively, we just scan all files to resolve that
        project.artifacts.add(libsrcConfig.name, 42) {
            it.builtBy(libsrcTask)
        }

        //TODO: no we can use that configuration to resolve tasks from IJ import
        configurations
            .getOrPut(forConfiguration) { LibSrcItem(forConfiguration) }
            .addTargets(files.toList())
    }
}

open class LibSrcItem(
    @Suppress("MemberVisibilityCanBePrivate")
    val configurationName: String,
) {
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Needed for Gradle to form NamedObjectCollection")
    val name by ::configurationName

    private var targets: Collection<Any> = emptyList()

    fun addTargets(targets: List<Any>) {
        this.targets += targets
    }

    override fun toString(): String {
        return "LibSrcItem(configuration='$configurationName', targets=$targets)"
    }
}

