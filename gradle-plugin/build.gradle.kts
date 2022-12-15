import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "6.1.0"
    `maven-publish`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin)
    id("com.gradle.plugin-publish") version "0.21.0"
}

repositories {
    maven("https://plugins.gradle.org/m2/")
}

val shadowImplementation: Configuration by configurations.creating
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)

dependencies {

    shadowImplementation(libs.kotlin.stdlib)
    shadowImplementation(project(":slimjar"))
    shadowImplementation("com.google.code.gson:gson:2.10")
    shadowImplementation(libs.kotlinx.coroutines)

    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly("com.github.jengelman.gradle.plugins:shadow:6.1.0")

    testImplementation("com.github.jengelman.gradle.plugins:shadow:6.1.0")
    testImplementation("org.assertj:assertj-core:3.23.1")

    // For grade log4j checker.
    configurations.onEach {
        it.exclude(group = "org.apache.logging.log4j", module = "log4j-core")
        it.exclude(group = "org.apache.logging.log4j", module = "log4j-api")
        it.exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j-impl")
    }
}

val shadowJarTask = tasks.named("shadowJar", ShadowJar::class.java)
val relocateShadowJar = tasks.register("relocateShadowJar", ConfigureShadowRelocation::class.java) {
    target = shadowJarTask.get()
}

shadowJarTask.configure {
    dependsOn(relocateShadowJar)
    archiveClassifier.set("")
    configurations = listOf(shadowImplementation)
}

kotlin {
    explicitApi()
}

// Required for plugin substitution to work in sample projects.
artifacts {
    add("runtimeOnly", shadowJarTask)
}

val ensureDependenciesAreInlined by tasks.registering {
    description = "Ensures all declared dependencies are inlined into shadowed jar"
    group = HelpTasksPlugin.HELP_GROUP
    dependsOn(tasks.shadowJar)

    doLast {
        val nonInlinedDependencies = mutableListOf<String>()
        zipTree(tasks.shadowJar.flatMap { it.archiveFile }).visit {
            if (!isDirectory) return@visit

            val path = relativePath
            if (
                !path.startsWith("META-INF") &&
                path.lastName.endsWith(".class") &&
                !path.pathString.startsWith("io.github.slimjar".replace(".", "/"))
            ) nonInlinedDependencies.add(path.pathString)
        }

        if (nonInlinedDependencies.isEmpty()) return@doLast
        throw GradleException("Found non inlined dependencies: $nonInlinedDependencies")
    }
}

tasks {
    named("check") {
        dependsOn(ensureDependenciesAreInlined)
        dependsOn(validatePlugins)
    }

    // Disabling default jar task as it is overridden by shadowJar
    named("jar") {
        enabled = false
    }

    whenTaskAdded {
        if (name == "publishPluginJar" || name == "generateMetadataFileForPluginMavenPublication") {
            dependsOn(named("shadowJar"))
        }
    }

    withType<GenerateModuleMetadata> {
        enabled = false
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            languageVersion = "1.7"
            freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
        }
    }

    withType<ShadowJar> {
        mapOf(
            "io.github.slimjar" to "",
            "me.lucko.jarrelocator" to ".jarrelocator",
            "com.google.gson" to ".gson",
            "kotlin" to ".kotlin",
            "org.intellij" to ".intellij",
            "org.jetbrains" to ".jetbrains"
        ).forEach { relocate(it.key, "io.github.slimjar${it.value}") }
    }

    test.get().useJUnitPlatform()
}

// Work around publishing shadow jars
afterEvaluate {
    publishing {
        publications {
            withType<MavenPublication> {
                if (name == "pluginMaven") {
                    setArtifacts(listOf(shadowJarTask.get()))
                }
            }
        }
    }
}

gradlePlugin {
    plugins {
        create("slimjar") {
            id = group.toString()
            displayName = "SlimJar"
            description = "JVM Runtime Dependency Management."
            implementationClass = "io.github.slimjar.SlimJarPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/DaRacci/slimjar"
    vcsUrl = "https://github.com/DaRacci/slimjar"
    tags = listOf("runtime dependency", "relocation")
    description = "Very easy to setup and downloads any public dependency at runtime!"
}
