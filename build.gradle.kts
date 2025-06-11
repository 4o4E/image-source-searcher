plugins {
    kotlin("jvm") version "2.1.21"
    `maven-publish`
    `java-library`
}

group = "top.e404"
version = "1.0.0"

repositories {
    mavenCentral()
}

fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"
fun ktor(module: String, version: String = "2.3.13") = "io.ktor:ktor-$module:$version"

dependencies {
    implementation(kotlin("reflect"))
    // coroutines
    implementation(kotlinx("coroutines-core-jvm", "1.10.2"))
    // ktor
    implementation(ktor("client-core-jvm"))
    implementation(ktor("client-okhttp-jvm"))
    implementation(ktor("client-apache"))
    implementation(ktor("client-content-negotiation"))
    implementation(ktor("serialization-kotlinx-json"))
    // nashorn
    runtimeOnly("org.openjdk.nashorn:nashorn-core:15.6")

    // jsoup
    implementation("org.jsoup:jsoup:1.17.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

java {
    withJavadocJar()
    withSourcesJar()
}

afterEvaluate {
    publishing.publications.create<MavenPublication>("java") {
        from(components["kotlin"])
        artifact(tasks.getByName("sourcesJar"))
        artifactId = project.name
        groupId = rootProject.group.toString()
        version = rootProject.version.toString()
    }
}