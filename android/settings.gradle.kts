pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Trackly"

include(":app")
include(":core:model")
include(":core:common")
include(":core:database")
include(":core:network")
include(":core:location")
include(":core:websocket")
include(":feature:auth")
include(":feature:customer-tracking")
include(":feature:driver-delivery")
include(":feature:ai-assistant")
