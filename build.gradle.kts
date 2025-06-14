plugins {
    kotlin("jvm") version "2.1.21"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.21")
    // https://mvnrepository.com/artifact/com.twelvemonkeys.imageio/imageio-core
    implementation("com.twelvemonkeys.imageio:imageio-core:3.11.0")
    // https://mvnrepository.com/artifact/com.twelvemonkeys.imageio/imageio-webp
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.11.0")
    // https://mvnrepository.com/artifact/com.formdev/flatlaf
    implementation("com.formdev:flatlaf:3.5.1")
    // https://mvnrepository.com/artifact/org.yaml/snakeyaml
    implementation("org.yaml:snakeyaml:2.3")
    implementation("info.picocli:picocli:4.7.6")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.17")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.5.18")
    runtimeOnly("com.github.gotson.nightmonkeys:imageio-heif:1.0.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(22)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.bluegecko.framelessviewer.MainKt"
    }
}