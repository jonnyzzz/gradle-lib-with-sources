@file:Suppress("HasPlatformType")

import java.io.ByteArrayOutputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

plugins {
    id("com.jonnyzzz.libsrc") version "0.0.1-SNAPSHOT"
    `java-library`
}

val libDir = File(projectDir ,"some-lib")
val srcDir = File(projectDir ,"some-src")

dependencies {
    implementation(fileTree(libDir) {
        include("*.jar")
        builtBy("pretendDownloadLibs")
    })
}

val pretendDownloadLibs by tasks.creating {
    outputs.dirs(libDir, srcDir)

    doFirst {
        delete(libDir, srcDir)
    }

    doLast {
        libDir.mkdirs()

        File(libDir, "api.jar").writeBytes(createJar {
            file("META-INF/info.txt", "this is example")
        })

        File(libDir, "impl.jar").writeBytes(createJar {
            file("META-INF/impl-info.txt", "this is example")
        })
    }

    doLast {
        srcDir.mkdirs()

        File(srcDir, "src/example.java").also {
            it.parentFile?.mkdirs()
            it.writeText("this is example")
        }

        File(srcDir, "src.jar").writeBytes(createJar {
            file("/a/b/c/d/impl-info.kt", "this is example")
        })
    }
}

interface JarBuilder {
    fun file(name: String, data: ByteArray)
    fun file(name: String, data: String) = file(name, data.toByteArray())
}

fun createJar(builder: JarBuilder.() -> Unit) : ByteArray {
    return ByteArrayOutputStream().also { bos ->
        JarOutputStream(bos).use { jar ->
            object: JarBuilder {
                override fun file(name: String, data: ByteArray) {
                    jar.putNextEntry(ZipEntry(name))
                    jar.write(data)
                    jar.closeEntry()
                }
            }.builder()
        }
    }.toByteArray()
}
