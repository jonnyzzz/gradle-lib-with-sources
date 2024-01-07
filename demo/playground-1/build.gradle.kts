@file:Suppress("HasPlatformType")

import de.undercouch.gradle.tasks.download.Download
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

plugins {
    id("com.jonnyzzz.libsrc") version "0.0.1-SNAPSHOT"
    `java-library`

    id("de.undercouch.download") version "5.5.0"
}

val libDir = File(projectDir, "some-lib")
val srcDir = File(projectDir, "some-src")
val srcZip = File(srcDir, "intellij-master.zip")
val srcUnpack = File(srcDir, "intellij")
val srcUnpackMarker = File(srcDir, "intellij.marker")

dependencies {
    implementation(fileTree(libDir) {
        include("*.jar")
        builtBy("pretendDownloadLibs")
    })
}

libsrc {
    implementation(srcUnpack) {
        builtBy(pretendDownloadLibs)
    }
}

val downloadIntelliJ by tasks.creating(Download::class) {
    src("https://github.com/JetBrains/intellij-community/archive/refs/heads/master.zip")
    dest(srcZip)
    overwrite(false)
}

val unpackIntelliJ by tasks.creating {
    dependsOn(downloadIntelliJ)

    inputs.file(srcZip)
    //old workaround to make gradle work faster on huge file trees
    outputs.file(srcUnpackMarker)

    doLast {
        delete(srcUnpack, srcUnpackMarker)
        copy {
            into(srcUnpack)
            from(zipTree(srcZip)) {
                eachFile { path = path.substringAfter('/') }
            }
        }
        srcUnpackMarker.parentFile?.mkdirs()
        srcUnpackMarker.writeText(Date().toString())
    }
}

val pretendDownloadLibs by tasks.creating {
    dependsOn(unpackIntelliJ)
}

interface JarBuilder {
    fun file(name: String, data: ByteArray)
    fun file(name: String, data: String) = file(name, data.toByteArray())
}

fun createJar(builder: JarBuilder.() -> Unit): ByteArray {
    return ByteArrayOutputStream().also { bos ->
        JarOutputStream(bos).use { jar ->
            object : JarBuilder {
                override fun file(name: String, data: ByteArray) {
                    jar.putNextEntry(ZipEntry(name))
                    jar.write(data)
                    jar.closeEntry()
                }
            }.builder()
        }
    }.toByteArray()
}
