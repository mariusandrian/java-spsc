plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.3"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    jmhImplementation("org.hdrhistogram:HdrHistogram:2.2.2")
}

tasks.test {
    useJUnitPlatform()
}