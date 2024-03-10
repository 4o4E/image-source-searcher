plugins {
    kotlin("jvm") version "1.9.22"
}

group = "top.e404"
version = "1.0.0"

repositories {
    mavenCentral()
}

fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"
fun ktor(module: String, version: String = "2.3.7") = "io.ktor:ktor-$module:$version"

dependencies {
    implementation(kotlin("reflect"))
    // coroutines
    implementation(kotlinx("coroutines-core-jvm", "1.8.0"))
    // ktor
    implementation(ktor("client-core-jvm"))
    implementation(ktor("client-okhttp-jvm"))
    implementation(ktor("client-apache"))
    implementation(ktor("client-content-negotiation"))
    implementation(ktor("serialization-kotlinx-json"))

    implementation("org.slf4j:slf4j-simple:2.0.12")
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.7")
    implementation("io.ktor:ktor-client-apache:2.3.7")
    // jsoup
    implementation("org.jsoup:jsoup:1.17.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}