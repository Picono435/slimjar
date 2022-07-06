import java.net.URI

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    java
    `maven-publish`
}

version = "1.2.6"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("me.lucko:jar-relocator:1.5")
    testImplementation("com.google.code.gson:gson:2.9.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:2.1.0")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.2")
    testImplementation("org.powermock:powermock-module-junit4:2.0.2")
    testImplementation("cglib:cglib:3.1")
}

publishing {
    publications.create("maven", MavenPublication::class) {
        from(components["java"])
        groupId = group.toString()
        artifactId = "slimjar"
        version = project.version.toString()
        pom {
            name.set("SlimJar")
            description.set("A simple and robust runtime dependency manager for JVM languages.")
            url.set("http://www.github.com/SlimJar/slimjar")
            licenses {
                license {
                    name.set("The MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("vshnv")
                    name.set("Vaishnav Anil")
                    email.set("vaishnavanil7th@gmail.com")
                }
                developer {
                    id.set("ipsk")
                    name.set("Mateus Moreira")
                }
            }
            scm {
                connection.set("https://github.com/SlimJar/slimjar")
                developerConnection.set("https://github.com/SlimJar/slimjar.git")
                url.set("https://github.com/SlimJar/slimjar")
            }
        }
    }

    repositories.maven {
        name = "RacciRepo"
        url = URI("https://repo.racci.dev/${if (project.version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"}")
        credentials(PasswordCredentials::class)
    }
}

tasks.jar {
    dependsOn(project(":loader-agent").tasks.jar)
    doFirst {
        copy {
            from(project(":loader-agent").tasks.getByName("jar").outputs.files.singleFile)
            into(layout.buildDirectory.file("resources/main/"))
            include("*.jar")
            rename("(.*)\\.jar", "loader-agent.isolated-jar")
        }
    }
}
