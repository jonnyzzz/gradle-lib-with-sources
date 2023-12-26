@file:Suppress("unused")

package com.jonnyzzz.libsrc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

@Suppress("unused")
class LibSrcPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hello from libsrc plugin")

        @Suppress("SpellCheckingInspection")
        val extension = project.dependencies.extensions.create("libsrc", LibSrcExt::class.java)
    }
}

open class LibSrcExt {
    private val configurations = mutableMapOf<String, LibSrcItem>()

    @Suppress("SpellCheckingInspection")
    fun libsrc(forConfiguration: String, vararg files: Any) {
        configurations
            .getOrPut(forConfiguration) { LibSrcItem(forConfiguration) }
            .addTargets(files.toList())
    }
}

open class LibSrcItem(
    @Suppress("MemberVisibilityCanBePrivate")
    val configurationName: String,
) {
    private var targets: Collection<Any> = emptyList()

    fun addTargets(targets: List<Any>) {
        this.targets += targets
    }

    override fun toString(): String {
        return "LibSrcItem(configuration='$configurationName', targets=$targets)"
    }
}

