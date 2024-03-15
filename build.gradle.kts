plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.2"
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

group = "com.romaindalichamp"
version = "1.3.2"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.3.4")
    type.set("IC")
    plugins.set(listOf("org.jetbrains.plugins.terminal"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("241.*")
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
    val mockitoVersion = "3+"
    val junitJupiter = "5.7.0"
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