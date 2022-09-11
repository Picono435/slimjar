import java.net.URI

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    java
    `maven-publish`
}

version = "1.2.8"

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
    testImplementation("org.junit.vintage:junit-vintage-engine:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core:4.7.0")
    testImplementation("org.mockito:mockito-inline:4.7.0")
    testImplementation("cglib:cglib:3.3.0")
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
            url.set("http://www.github.com/DaRacci/slimjar")
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
                    roles.set(listOf("Project starter"))
                }
                developer {
                    id.set("ipsk")
                    name.set("Mateus Moreira")
                    roles.set(listOf("Previous Maintainer"))
                }
                developer {
                    id.set("Racci")
                    name.set("James Draycott")
                    email.set("racci@racci.dev")
                    roles.set(listOf("MAINTAINER"))
                }
            }
            scm {
                connection.set("https://github.com/DaRacci/slimjar")
                developerConnection.set("https://github.com/DaRacci/slimjar.git")
                url.set("https://github.com/DaRacci/slimjar")
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
