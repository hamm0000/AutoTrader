plugins {
    id("java")
}

group = "dev.hamm0000"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework:spring-framework-bom:6.2.1"))
    implementation("org.springframework:spring-web")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    testImplementation("org.springframework:spring-test")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
