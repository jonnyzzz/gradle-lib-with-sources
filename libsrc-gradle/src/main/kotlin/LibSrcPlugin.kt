@file:Suppress("unused")

package com.jonnyzzz.libsrc

import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionAware
import java.util.concurrent.CopyOnWriteArrayList

@Suppress("unused")
class LibSrcPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hello from libsrc plugin")

        val libsrcConfig: Configuration = project.configurations.create("libsrc-configuration")
        libsrcConfig.isVisible = false

        val ext = project.extensions.create(
            "libsrc", LibSrcExt::class.java
        )

        project.configurations.all {
            if (it.isVisible) {
                println("Configure for :${it.name}")

                ext.extensions.create(it.name, LibSrcItem::class.java, project, ConfigurationName(it.name))
            }
        }

    }
}

data class ConfigurationName(val name: String)

abstract class LibSrcExt : ExtensionAware

abstract class LibSrcItem(
    private val project: Project,

    @Suppress("MemberVisibilityCanBePrivate")
    private val configurationName: ConfigurationName,
) : Named {
    companion object {
        private val log = Logging.getLogger(LibSrcItem::class.java)
    }

    private val targets = CopyOnWriteArrayList<ConfigurableFileTree>()

    //this is needed to implement Named interface from Gradle
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Use configurationName instead")
    override fun getName(): String = configurationName.name

    /**
     * Attaches a sources set via [ConfigurableFileTree] for this configuration.
     *
     * Use [ConfigurableFileTree.builtBy] to define task dependencies
     * to be evaluated on project import in IntelliJ
     *
     * The configuration is lazy to evaluate, it uses [Project.fileTree] and [Project.file]
     * to resolve the sources.
     */
    open operator fun invoke(
        baseDir: Any,
        action: Action<in ConfigurableFileTree> = Action { }
    ): ConfigurableFileTree {
        log.info("Configuring libsrc for configuration {} and baseDir {}", configurationName.name, baseDir)

        val tree = project.fileTree(baseDir, action)
        targets += tree
        return tree
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(configuration='$configurationName', targets=$targets)"
    }
}
