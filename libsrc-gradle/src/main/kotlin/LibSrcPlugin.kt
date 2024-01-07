@file:Suppress("unused")

package com.jonnyzzz.libsrc

import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.Logging
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

@Suppress("unused")
class LibSrcPlugin : Plugin<Project> {
    companion object {
        private val log = Logging.getLogger(LibSrcPlugin::class.java)
    }

    override fun apply(project: Project) {
        val configurations = project.objects.domainObjectContainer(LibSrcItem::class.java) {
            throw UnsupportedOperationException("Project configurations are mapped automatically to libSrc items")
        }

        project.extensions.create(
            LibSrcExtension::class.java,
            "libsrc",
            LibSrcExtImpl::class.java, configurations
        )

        project.configurations.all {
            if (it.isVisible) {
                log.info("Configure for configuration {}", it.name)
                val newItem = project.objects.newInstance(LibSrcItem::class.java, project, it.name)
                configurations.add(newItem)
            }
        }
    }
}

interface LibSrcExtension : NamedDomainObjectContainer<LibSrcItem> {
    /**
     * Attaches sources to the configuration.
     *
     * @see LibSrcItem.invoke for more details
     */
    operator fun NamedDomainObjectProvider<LibSrcItem>.invoke(
        baseDir: Any,
        action: Action<in ConfigurableFileTree> = Action { }
    ) = configure { it(baseDir, action) }

    /**
     * Attaches sources to the configuration.
     *
     * Helper function to support cases, where Gradle code generation
     * is not enabled.
     *
     * @see LibSrcItem.invoke for more details
     */
    operator fun String.invoke(
        baseDir: Any,
        action: Action<in ConfigurableFileTree> = Action { }
    ) = named(this).configure { it(baseDir, action) }
}

open class LibSrcExtImpl(
    private val configurations: NamedDomainObjectContainer<LibSrcItem>,
) : LibSrcExtension, NamedDomainObjectContainer<LibSrcItem> by configurations

open class LibSrcItem @Inject constructor(
    private val project: Project,

    @Suppress("MemberVisibilityCanBePrivate")
    val configurationName: String,
) : Named {
    companion object {
        private val log = Logging.getLogger(LibSrcItem::class.java)
    }

    private val targets = CopyOnWriteArrayList<ConfigurableFileTree>()

    //this is needed to implement Named interface from Gradle
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Use configurationName instead")
    override fun getName(): String = configurationName

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
        log.info("Configuring libsrc for configuration {} and baseDir {}", configurationName, baseDir)

        val tree = project.fileTree(baseDir, action)
        targets += tree
        return tree
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(configuration='$configurationName', targets=$targets)"
    }
}
