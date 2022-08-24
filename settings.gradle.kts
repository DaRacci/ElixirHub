enableFeaturePreview("VERSION_CATALOGS")
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.racci.dev/releases")
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    plugins {
        val kotlinVersion: String by settings
        kotlin("plugin.serialization") version kotlinVersion
    }

    resolutionStrategy {
        val minixConventions: String by settings
        eachPlugin {
            if (requested.id.id.startsWith("dev.racci.minix")) {
                useVersion(minixConventions)
            }
        }
    }
}

// Version Catalogs
dependencyResolutionManagement {
    repositories.maven("https://repo.racci.dev/releases")

    versionCatalogs.create("lib") {
        val minixConventions: String by settings
        from("dev.racci:catalog:$minixConventions")
    }
}

rootProject.name = "ElixirHub"
