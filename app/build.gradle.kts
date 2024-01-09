import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    application
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    mavenNav("paw-arbeidssokerregisteret")
}

val arbeidssokerregisteretVersion = "23.12.18.110-1"
val navCommonModulesVersion = "2.2023.01.02_13.51-1c6adeb1653b"
val logstashVersion = "7.3"
val logbackVersion = "1.4.12"
val pawUtilsVersion = "23.12.20.5-1"

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema("no.nav.paw.arbeidssokerregisteret.api.schema:eksternt-api:$arbeidssokerregisteretVersion")
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")

    //logging
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    //Kafka
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")
    implementation("io.confluent:kafka-avro-serializer:7.5.3")
    implementation("org.apache.avro:avro:1.11.3")
    implementation("org.apache.kafka:kafka-clients:3.6.0")

    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Use the JUnit 5 integration.
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source(zipTree(schema.singleFile))
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.profilering.AppKt")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Title"] = rootProject.name
        attributes["Main-Class"] = application.mainClass.get()
    }
}

fun RepositoryHandler.mavenNav(repo: String): MavenArtifactRepository {
    val githubPassword: String by project

    return maven {
        setUrl("https://maven.pkg.github.com/navikt/$repo")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}
