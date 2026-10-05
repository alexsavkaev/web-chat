plugins {
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.serialization") version "2.2.20" apply false
}

allprojects {
    group = "chat.web"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
