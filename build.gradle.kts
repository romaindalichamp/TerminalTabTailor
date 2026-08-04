import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.5"
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
}

group = "com.romaindalichamp"
version = "1.5.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
        bundledPlugin("org.jetbrains.plugins.terminal")
    }

    val mockitoVersion = "5.5.0"
    val junitJupiter = "5.10.0"
    val kotlinTestJunit = "2.1.0"
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiter")
    // The IntelliJ test framework initialises its Logger through JUnit 4 classes even when the
    // tests themselves are Jupiter, so junit4 must stay on the test runtime classpath.
    testRuntimeOnly("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiter")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinTestJunit")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
            // No upper bound, so a new IDE major does not require a release every time.
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }

    buildSearchableOptions {
        // The headless IDE this task boots reports leaked Configurable panels on shutdown
        // (1200+ SEVERE lines), all of them from the platform and bundled plugins.
        jvmArgs("-Didea.is.internal=false")
    }

    test {
        useJUnitPlatform()
    }
}
