plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
    application
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

val navCommonModulesVersion = "2.2023.01.02_13.51-1c6adeb1653b"
val logstashVersion = "7.3"
val logbackVersion = "1.4.5"

dependencies {
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("io.confluent:kafka-avro-serializer:7.4.0")

    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Use the JUnit 5 integration.
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

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
    // Define the main class for the application.
    mainClass.set("no.nav.paw.arbeidssokerregisteret.profilering.AppKt")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

//tasks.withType(Jar::class) {
//    manifest {
//        attributes["Implementation-Version"] = project.version
//    }
//}
