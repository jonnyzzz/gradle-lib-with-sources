package com.jonnyzzz.libsrc

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class LibSrcPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hello from libsrc plugin")
    }
}

