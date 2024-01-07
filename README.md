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




# Related Bug

## Gradle configuration
A dependency like 
```
dependencies {
  implementation(provider { fileTree(..) }) { /* this parameter causes crash */ }
}
```
crashes with 
java.lang.ClassCastException: class org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency_Decorated cannot be cast to class org.gradle.api.artifacts.ExternalModuleDependency

## IntelliJ import vs builtBy
A `fileTree`'s `buildBy` is ignored by IDE import. You can see that in the following
example (as playground-2):

```kotlin
plugins {
    `java-library`
}
val buildMe by tasks.creating {
    doLast {
        error("import must fail")
    }
}
dependencies {
    implementation(fileTree(projectDir) {
        builtBy(buildMe)
    })
}
```
This project must not complete importing, but so far it works. 

It has a
[shelved IDEA-167533](https://youtrack.jetbrains.com/issue/IDEA-167533/Importing-Gradle-project-misses-tasks-that-use-builtBy)
bug for IntelliJ.


# Implementing Gradle Extensions

For the `libsrc` we need an extension which works just like the `dependencies` block for standard configurations. 
You may see from the Gradle sources, that it generated for every configuration a set of extension functions,
for all registered configurations. That logic is implemented as a custom case of [AccessorFragments.kt](https://github.com/gradle/gradle/blob/2c09566a23addb5640baaa3347c17d0e80ce416d/platforms/core-configuration/kotlin-dsl/src/main/kotlin/org/gradle/kotlin/dsl/accessors/AccessorFragments.kt#L58),
see [AccessorFragments for Configurations](https://github.com/gradle/gradle/blob/2c09566a23addb5640baaa3347c17d0e80ce416d/platforms/core-configuration/kotlin-dsl/src/main/kotlin/org/gradle/kotlin/dsl/accessors/AccessorFragments.kt#L68

Our plugin uses `project.configurations.all { }` to generate all elements to the collection of our extensions.

Gradle use the following logic to collect all the targets for code generation:
[DefaultProjectSchemaProvider](https://github.com/gradle/gradle/blob/783e4aa305d675d10e2f5f56f2c5794d15356689/platforms/core-configuration/kotlin-dsl-provider-plugins/src/main/kotlin/org/gradle/kotlin/dsl/provider/plugins/DefaultProjectSchemaProvider.kt#L70).

Ideally, we need two accessors: a property getter and a function that accepts configuration lambda. 

The best is to implement a deprecated `Convetion`, for which Gradle generates exactly 2 getters. It is not
a best way because it is deprecated. 

From the code, which builds the model, we can [see]([DefaultProjectSchemaProvider](https://github.com/gradle/gradle/blob/783e4aa305d675d10e2f5f56f2c5794d15356689/platforms/core-configuration/kotlin-dsl-provider-plugins/src/main/kotlin/org/gradle/kotlin/dsl/provider/plugins/DefaultProjectSchemaProvider.kt#L70).
) that Gradle generates accessors for all extensions which implement `NamedDomainObjectContainer` interface. 

I found the way to create an instance of `NamedDomainObjectContainer`, just use `project.objects.domainObjectContainer`
function to generate one. I also use delegated implementation to make my extension object to implenent that
interface. Outcome -- we have a getter per each collection element.

Now I found that `NamedDomainObjectContainer` is probably not the best option, because we know all the
possible elements of that collection beforehand, more specifically, there has to be an element for each
Configuration of the project. 

Next approach is to make our extension object implement `ExtensionAware` interface. Gradle automatically
generates all necessary implementation methods, e.g. `abstract class MyExtension: ExtensionAware` is enough.
Similarly, we register an extension per each known configuration. The downside if that approach is that
there is no base type for extensions, so one would have do deal with that somehow when/if it's needed to
list all configured extensions.

