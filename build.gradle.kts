plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.bouncycastle:bcprov-jdk18on:1.81")
    implementation("io.ktor:ktor-client-core:3.3.0")
    implementation("io.ktor:ktor-client-cio:3.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
    implementation("io.ktor:ktor-client-websockets:3.3.0") // Added this line
    implementation("io.ktor:ktor-serialization-gson:3.3.0") // You might not need this if fully migrated to kotlinx-serialization for Ktor
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.google.genai:google-genai:1.17.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("org.web3j:crypto:4.10.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "bot.MainKt" // TODO: Specify your main class
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/SIG-*") // Specifically for BouncyCastle and similar
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory = project.layout.projectDirectory.asFile
    archiveFileName.set("galactictide.jar")
}