plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("io.javalin:javalin:7.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    //Base de datos y ORM
    implementation("com.h2database:h2:2.3.232")
    implementation("org.hibernate.orm:hibernate-core:6.6.0.Final")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.pucmm.eventos.Main"
    }
}