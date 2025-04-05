import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "substitutemanager"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
    
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("substitutemanager.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "substitutemanager.MainKt"
    }
    
    // Include all dependencies in the jar
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Task to copy data files to build directory
tasks.register<Copy>("copyDataFiles") {
    from("../data")
    into("${buildDir}/resources/main/data")
}

// Ensure data files are copied before running
tasks.named("run") {
    dependsOn("copyDataFiles")
}

// Ensure data files are copied before creating a JAR
tasks.named("jar") {
    dependsOn("copyDataFiles")
}
