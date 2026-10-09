plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    application
}

group = "hu.zoltanegyhazi.cartinder"
version = "1.0"

val ktor = "3.3.3"
val exposed = "1.2.0"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("io.ktor:ktor-server-auth:$ktor")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor")
    implementation("io.ktor:ktor-server-status-pages:$ktor")
    implementation("io.ktor:ktor-server-call-logging:$ktor")
    implementation("org.jetbrains.exposed:exposed-core:$exposed")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("ch.qos.logback:logback-classic:1.5.38")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktor")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("hu.zoltanegyhazi.cartinder.server.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}
