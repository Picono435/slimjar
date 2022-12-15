import java.net.URI

plugins {
    java
    id("com.github.hierynomus.license-base") version "0.15.0"
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

    dependencies {
        implementation(kotlin("bom:${rootProject.libs.versions.kotlin}")) // Keep kotlin versions in sync.

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
