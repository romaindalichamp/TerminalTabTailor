plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.2"
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

group = "com.romaindalichamp"
version = "1.4.3"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.3.1")
    type.set("IC")
    plugins.set(listOf("org.jetbrains.plugins.terminal"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
dependencies {
    val mockitoVersion = "5.5.0"
    val junitJupiter = "5.10.0"
    val kotlinTestJunit = "2.0.0-Beta3"
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiter")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinTestJunit")
}

tasks.test {
    useJUnitPlatform()
}