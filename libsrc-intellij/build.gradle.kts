import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java") // Java support
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.16.1"
}

///looks like IJ SDK bug/feature, because setting is NOT inherited from parent project
repositories {
    mavenCentral()
}

dependencies {

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-no-stdlib")
    }
}

intellij {
    pluginName = "libsrc"
    version = "2023.3.2" //aka 2023.3.2
    type = "IC"

    plugins.add("gradle")
}

tasks.patchPluginXml {
    sinceBuild = "223.*"
    untilBuild = "999.*"
}


// Configure UI tests plugin
// Read more: https://github.com/JetBrains/intellij-ui-test-robot
//tasks.runIdeForUiTests {
//    systemProperty("robot-server.port", "8082")
//    systemProperty("ide.mac.message.dialogs.as.sheets", "false")
//    systemProperty("jb.privacy.policy.text", "<!--999.999-->")
//    systemProperty("jb.consents.confirmation.enabled", "false")
//}
//
//tasks.signPlugin {
//    certificateChain = environment("CERTIFICATE_CHAIN")
//    privateKey = environment("PRIVATE_KEY")
//    password = environment("PRIVATE_KEY_PASSWORD")
//}
//
//tasks.publishPlugin {
//    dependsOn("patchChangelog")
//    token = environment("PUBLISH_TOKEN")
    // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
//    channels = properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) }
//}
