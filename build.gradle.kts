

subprojects {
    group = "com.jonnyzzz.libsrc"
    version = System.getenv("BUILD_NUMBER") ?: "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
