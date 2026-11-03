plugins {
    id("java")
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "ai.assistant.improver"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion").get())
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

intellijPlatform {
    pluginConfiguration {
        id = "ai.assistant.improver"
        name = "AI Assistant Improver"
        version = "0.1.0"
        description = "Installs skills, agents, and rules from git repositories into the enabled coding assistants."
        ideaVersion {
            sinceBuild = providers.gradleProperty("sinceBuild")
            untilBuild = provider { null }
        }
        vendor {
            name = "AI Assistant Improver"
        }
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
    doFirst {
        jvmArgumentProviders.clear()
        systemProperties.keys.removeIf { key ->
            key.startsWith("idea.") || key == "java.system.class.loader"
        }
        jvmArgs = jvmArgs.orEmpty().filterNot { arg ->
            arg.startsWith("-Didea.") || arg.contains("java.system.class.loader")
        }
    }
}
