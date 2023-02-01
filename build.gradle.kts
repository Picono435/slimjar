import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import java.net.URI

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    id("com.github.hierynomus.license-base") version "0.16.1"
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "com.github.hierynomus.license-base")

    repositories {
        mavenCentral()
    }

    license {
        header = rootProject.file("LICENSE")
        includes(listOf("**/*.java', '**/*.kt"))
        mapping("kt", "DOUBLESLASH_STYLE")
        mapping("java", "DOUBLESLASH_STYLE")
    }

    plugins.withType<KotlinBasePlugin> {
        extensions.configure<KotlinProjectExtension> {
            jvmToolchain(17)
            explicitApi()
        }
    }

    dependencies {
//        implementation(kotlin("bom:${rootProject.libs.versions.kotlin.get()}")) // Keep kotlin versions in sync.

        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    }

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

    afterEvaluate {
        plugins.withType<MavenPublishPlugin> {
            extensions.getByName<PublishingExtension>("publishing").apply {
                repositories.maven {
                    name = "RacciRepo"
                    url = URI("https://repo.racci.dev/${if (project.version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"}")
                    credentials(PasswordCredentials::class)
                }
            }
        }
    }
}
