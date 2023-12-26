package com.jonnyzzz.libsrc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

@Suppress("unused")
class LibSrcPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hello from libsrc plugin")

        @Suppress("SpellCheckingInspection")
        val extension = project.extensions.create("libsrc", LibSrcExt::class.java)
    }
}

open class LibSrcExt {
}

