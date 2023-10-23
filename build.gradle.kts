plugins {
    val kotlinVersion = "1.5.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.9.2"
}

dependencies {
    implementation("org.jsoup:jsoup:1.14.1")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.seleniumhq.selenium:selenium-java:3.141.59")
//    implementation("org.seleniumhq.selenium.selenium-server:3.141.59")
}

group = "site.longint"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.aliyun.com/repository/spring")
    maven("https://maven.aliyun.com/repository/google")
    mavenCentral()
}
