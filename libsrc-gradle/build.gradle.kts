plugins {
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "1.8.22"
}

gradlePlugin {
    // Define the plugin
    val greeting by plugins.creating {
        id = "com.jonnyzzz.libsrc"
        implementationClass = "com.jonnyzzz.libsrc.LibSrcPlugin"
    }
}

java {
    withSourcesJar()
}

publishing {
    repositories {
        mavenLocal()
    }

    publications {
        create<MavenPublication>("libsrc") {
            from(components["java"])
        }
    }
}
