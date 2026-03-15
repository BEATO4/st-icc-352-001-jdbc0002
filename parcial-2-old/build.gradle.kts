import sun.jvmstat.monitor.MonitoredVmUtil.mainClass

plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.pucmm"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "java.org.pucmm.eventos.Main"
}

repositories {
    mavenCentral()
}

dependencies {
    // Javalin 7
    implementation("io.javalin:javalin:7.3.4")

    // Jackson JSON + Java 8 Time
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.8")

    // Hibernate ORM 6
    implementation("org.hibernate.orm:hibernate-core:6.6.3.Final")

    // H2 Database (server mode)
    implementation("com.h2database:h2:2.3.232")

    // QR Code generation (ZXing)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    // BCrypt password hashing
    implementation("org.mindrot:jbcrypt:0.4")
}

tasks.shadowJar {
    archiveBaseName.set("eventos")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    manifest {
        attributes["Main-Class"] = "org.pucmm.eventos.Main"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}