plugins {
    java
}

group = "dev.potatosmp.potato_stats"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
        content {
            includeModule("io.papermc.paper", "paper-api")
            includeModule("net.md-5", "bungeecord-chat")
            includeGroup("io.papermc.adventure")
        }
    }

    maven {
        name = "minecraft"
        url = uri("https://libraries.minecraft.net")
        content {
            includeModule("com.mojang", "brigadier")
        }
    }

    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.214-stable")
}

tasks.jar {
    archiveBaseName.set("potato-stats")
    manifest {
        attributes("Main-Class" to "dev.potatosmp.potato_stats.PotatoStatsPlugin")
    }
    from(sourceSets.main.get().output)
    dependsOn(tasks.processResources)
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.named<JavaCompile>("compileTestJava") {
    options.encoding = "UTF-8"
    options.release.set(25)
}
