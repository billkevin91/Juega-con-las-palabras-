plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
}

val gdxVersion = "1.12.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    named("main") {
        kotlin.setSrcDirs(listOf("src/main/kotlin"))
    }
}

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
}
