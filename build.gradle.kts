plugins {
    java
}

group = "fun.eightxm"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/") // WorldEdit/FAWE
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.15.0")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.15.0") { isTransitive = false }
    implementation("com.github.Querz:NBT:6.1")
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.jar {
    // Relocate/shade dependencies into the jar
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
