plugins {
    id("java")
    id ("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.pucmm.blog.Main")
}

dependencies {
    // --- Dependencias Originales ---
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("io.javalin:javalin:6.7.0")
    implementation("io.javalin:javalin-rendering:6.1.3")
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    // --- NUEVAS DEPENDENCIAS ---

    // 1. Hibernate (ORM y JPA)
    implementation("org.hibernate.orm:hibernate-core:6.4.4.Final")

    // 2. Base de datos H2
    implementation("com.h2database:h2:2.2.224")

    // 3. Driver de PostgreSQL
    implementation("org.postgresql:postgresql:42.7.3")

    // 4. Jasypt
    implementation("org.jasypt:jasypt:1.9.3")
}

tasks.test {
    useJUnitPlatform()
}