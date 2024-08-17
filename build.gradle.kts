plugins {
//    val kotlinVersion = "1.5.30"
    val kotlinVersion = "1.8.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.15.0"
}

dependencies {
    implementation("org.jsoup:jsoup:1.14.1")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.seleniumhq.selenium:selenium-java:3.141.59")
//    implementation("org.seleniumhq.selenium.selenium-server:3.141.59")
}

repositories {
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.aliyun.com/repository/spring")
    maven("https://maven.aliyun.com/repository/google")
    mavenCentral()
}

// https://github.com/gnuf0rce/debug-helper/blob/main/src/main/kotlin/io/github/gnuf0rce/mirai/debug/command/DebugCommands.kt
group = "site.longint"
version = "1.1.17"
// 1.0-QA
// 1.1-welcome
// 1.2-adAnti(jar: 2.9->2.11)
