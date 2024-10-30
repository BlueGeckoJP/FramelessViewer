plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.bluegecko"
version = "latest"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0-RC1")
    // https://mvnrepository.com/artifact/com.twelvemonkeys.imageio/imageio-core
    implementation("com.twelvemonkeys.imageio:imageio-core:3.11.0")
    // https://mvnrepository.com/artifact/com.twelvemonkeys.imageio/imageio-webp
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.11.0")
    // https://mvnrepository.com/artifact/com.formdev/flatlaf
    implementation("com.formdev:flatlaf:3.5.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.bluegecko.framelessviewer.MainKt"
    }
}