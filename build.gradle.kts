@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import net.kyori.indra.git.IndraGitExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import java.time.Instant

plugins {
    alias(libs.plugins.indra.git)
    alias(libs.plugins.jib)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.spotless)
    `java-base`
}

val javaTarget = 21
val entryPoint = "io.ktor.server.netty.EngineMain"

java {
    val targetVersion = JavaVersion.toVersion(javaTarget)
    sourceCompatibility = targetVersion
    targetCompatibility = targetVersion
    if (JavaVersion.current() < targetVersion) {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaTarget)) }
        kotlin.jvmToolchain(javaTarget)
    }
}

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots"
        mavenContent {
            snapshotsOnly()
        }
    }
}

spotless {
    fun com.diffplug.gradle.spotless.FormatExtension.setup() {
        endWithNewline()
        trimTrailingWhitespace()
    }
    kotlin {
        setup()
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        setup()
        ktlint(libs.versions.ktlint.get())
    }
}

kotlin {
    explicitApi()

    jvm {
        withJava()
        mainRun {
            mainClass = entryPoint
        }
        compilerOptions {
            jvmTarget =
                org.jetbrains.kotlin.gradle.dsl.JvmTarget
                    .fromTarget("$javaTarget")
            freeCompilerArgs.add("-Xjdk-release=$javaTarget")
        }
    }

    js {
        browser {
        }
        compilerOptions {
            target = "es2015"
        }
        binaries.executable()
    }

    sourceSets {
        configureEach {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }

        named("commonMain") {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.html)
            }
        }

        named("jvmMain") {
            dependencies {
                implementation(libs.bundles.ktor.server)
                implementation(libs.bundles.ktor.client)
                implementation(libs.ktor.network)

                implementation(libs.adventure.minimessage)
                implementation(libs.adventure.text.serializer.gson)
                implementation(libs.adventure.text.serializer.legacy)
                implementation(libs.cache4k)
                implementation(libs.logback.classic)
            }
        }
    }
}

jib {
    from {
        image = "azul/zulu-openjdk-alpine:$javaTarget-jre"
        platforms {
            // We can only build multi-arch images when pushing to a registry, not when building locally
            val requestedTasks = gradle.startParameter.taskNames
            if ("jibBuildTar" in requestedTasks || "jibDockerBuild" in requestedTasks) {
                platform {
                    // todo: better logic
                    architecture =
                        when (System.getProperty("os.arch")) {
                            "aarch64" -> "arm64"
                            else -> "amd64"
                        }
                    os = "linux"
                }
            } else {
                platform {
                    architecture = "amd64"
                    os = "linux"
                }
                platform {
                    architecture = "arm64"
                    os = "linux"
                }
            }
        }
    }
    container {
        setMainClass(entryPoint)
        labels.put(
            "org.opencontainers.image.description",
            """A Web UI for working with Adventure components.
            Built with Adventure ${libs.versions.adventure.get()}, from webui commit ${indraGit.commit()?.name ?: "<unknown>"}""",
        )
    }
    to {
        image = "ghcr.io/papermc/adventure-webui/webui"
        tags =
            setOf(
                "latest",
                "${indraGit.branchName()}-${indraGit.commit()?.name()?.take(7)}-${Instant.now().epochSecond}",
            )
    }
}

tasks {
    val webpackTask =
        if (isDevelopment()) {
            "jsBrowserDevelopmentWebpack"
        } else {
            "jsBrowserProductionWebpack"
        }.let { taskName ->
            named<KotlinWebpack>(taskName)
        }

    named<Jar>("jvmJar") {
        rootProject.indraGit.applyVcsInformationToManifest(manifest)
    }

    named<AbstractCopyTask>("jvmProcessResources") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(webpackTask.flatMap { it.mainOutputFile })
        filesMatching("application.conf") {
            expand(
                "jsScriptFile" to "${rootProject.name}.js",
                "miniMessageVersion" to
                    libs.adventure.minimessage
                        .get()
                        .versionConstraint.requiredVersion,
                "commitHash" to
                    rootProject.extensions
                        .getByType<IndraGitExtension>()
                        .commit()
                        ?.name
                        .orEmpty(),
            )
        }
    }

    // the kotlin plugin creates this task super late for some reason?
    configureEach {
        if (name == "jvmRun" && isDevelopment()) {
            (this as JavaExec).jvmArgs("-Dio.ktor.development=true", "-DwebuiLogLevel=trace")
        }
    }
}

/** Checks if the development property is set. */
fun isDevelopment(): Boolean = project.hasProperty("isDevelopment")
