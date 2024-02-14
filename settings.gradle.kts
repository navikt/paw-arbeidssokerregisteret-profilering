rootProject.name = "paw-arbeidssokerregisteret-profilering"
include("app")

dependencyResolutionManagement {
    val githubPassword: String by settings
    repositories {
        maven {
            setUrl("https://maven.pkg.github.com/navikt/paw-arbeidssokerregisteret-api")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
    }
    versionCatalogs {
        create("pawObservability") {
            from("no.nav.paw.observability:observability-version-catalog:23.10.25.8-1")
        }
    }
}

