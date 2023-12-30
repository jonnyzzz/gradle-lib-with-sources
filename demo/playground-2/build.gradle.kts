plugins {
    `java-library`
}

val buildMe by tasks.creating {
    outputs.upToDateWhen { false }
    doLast {
        println("This task is called on import")
    }
}

dependencies {
    implementation(fileTree(projectDir) {
        builtBy(buildMe)
    })
}
