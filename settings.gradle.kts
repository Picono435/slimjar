enableFeaturePreview("VERSION_CATALOGS")

rootProject.name = "slimjar"

include("slimjar", "slimjar-external", "gradle-plugin", "loader-agent")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.racci.dev/releases") { mavenContent { releasesOnly() } }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.racci.dev/releases") { mavenContent { releasesOnly() } }
    }

    versionCatalogs.create("libs") {
        val kotlinVersion: String by settings
        val build: String by settings
        val conventions = kotlinVersion.plus("-").plus(build)
        from("dev.racci:catalog:$conventions")
    }
}
