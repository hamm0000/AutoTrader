plugins {
    id("java")
    id("org.springframework.boot") version "4.1.0"
}

group = "dev.hamm0000"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))
    implementation(project(":trading"))
    implementation(project(":adapter-kis"))
    implementation(project(":adapter-strategy"))
    implementation("org.springframework.boot:spring-boot-starter-restclient")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
