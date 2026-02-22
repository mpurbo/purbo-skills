plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    // Kafka Streams
    implementation("org.apache.kafka:kafka-streams:3.7.0")

    // Serdes — pick one or both depending on schema strategy
    implementation("io.confluent:kafka-streams-avro-serde:7.6.0")
    // implementation("io.confluent:kafka-json-schema-serializer:7.6.0")

    // Jackson for JSON serdes (if not using Schema Registry)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")

    // Config
    implementation("com.typesafe:config:1.4.3")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.12")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")

    // Testing — pure core (no Kafka dependency)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation(kotlin("test"))

    // Testing — topology (in-process, no real broker)
    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.7.0")

    // Testing — integration (real broker via Testcontainers, optional)
    testImplementation("org.testcontainers:kafka:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
}

application {
    mainClass.set("com.example.service.AppKt")
}

tasks.test {
    useJUnitPlatform()

    // Run pure core tests first (fast), topology tests second
    // Tag-based ordering: core tests have no special tag,
    // topology tests use @Tag("topology"),
    // integration tests use @Tag("integration")
}

// Separate test tasks for CI pipeline
tasks.register<Test>("unitTest") {
    useJUnitPlatform {
        excludeTags("topology", "integration")
    }
    description = "Run pure core unit tests only (fast, no infra)"
}

tasks.register<Test>("topologyTest") {
    useJUnitPlatform {
        includeTags("topology")
    }
    description = "Run topology wiring tests (TopologyTestDriver, no real broker)"
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    description = "Run integration tests (requires Docker for Testcontainers)"
}

kotlin {
    jvmToolchain(17)
}
