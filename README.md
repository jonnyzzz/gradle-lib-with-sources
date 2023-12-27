# gradle-lib-with-sources

# What and Why

We introduce a Gradle Plugin + IntelliJ plugin to allow **attaching sources to library folder dependencies**,
which one needed to deal with a non-maven applications, for example:

```kotlin
dependencies {
    //the to add dependency to some JAR files
    implementation(fileTree(someLibDir) { builtBy(someTask) })
}
```

There is no standard way for IntelliJ ecosystem to include sources
when the project is opened in the IDE. *We are fixing it* 

# Implementation Details

The `implementation(..)` function call above returns a `Dependency` instance, 
we see that it's type (at least with Gradle 8.5) is
`org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency_Decorated`.

Similarly, we can see the same from the [Gradle source code](https://github.com/gradle/gradle/blob/5bb3182cf38a901dbffbacc0cb9c8efec9f87e9a/platforms/software/dependency-management/src/main/java/org/gradle/api/internal/notations/DependencyNotationParser.java#L63).

The [DefaultSelfResolvingDependency](https://github.com/gradle/gradle/blob/66c05cb15569a9c0fd26dbac969b33b843346d69/subprojects/core/src/main/java/org/gradle/api/internal/artifacts/dependencies/DefaultSelfResolvingDependency.java)
class does not allow to provide maven coorinates for such a dependency. 

The IntelliJ logic of importing if found in the
[CommonGradleProjectResolverExtension](https://github.com/JetBrains/intellij-community/blob/5ce332e3df506d099d49568ee7286c5d19de6273/plugins/gradle/src/org/jetbrains/plugins/gradle/service/project/CommonGradleProjectResolverExtension.java#L84),
where we can see there is a specific logic for dependencies without coordinates.
**Note**, project-related dependencies will import as module libraries to IntelliJ. 

IntelliJ-Gradle import does not return any sources via 
[IdeaSingleEntryLibraryDependency#getSource](https://github.com/JetBrains/intellij-community/blob/5ce332e3df506d099d49568ee7286c5d19de6273/plugins/gradle/src/org/jetbrains/plugins/gradle/service/project/CommonGradleProjectResolverExtension.java#L84)
call for such case. **TODO: Check that**

## Known Workarounds and Issues

TODO: Explain IntelliJ SDK plugin approach via Ivy Module. 

TODO: check YouTrack for issues.



