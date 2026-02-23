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
    mainClass.set("edu.pucmm.icc352.Main")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("io.javalin:javalin:6.7.0")
    implementation("io.javalin:javalin-rendering:6.1.3")
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
    implementation("org.slf4j:slf4j-simple:2.0.7")

}

tasks.test {
    useJUnitPlatform()
}