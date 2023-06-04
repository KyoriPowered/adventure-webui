import net.kyori.indra.git.IndraGitExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

@Suppress("DSL_SCOPE_VIOLATION") // https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    application
    alias(libs.plugins.indra.git)
    alias(libs.plugins.jib)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.spotless)
}

val javaTarget = 11
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
        compilations.configureEach {
            kotlinOptions.jvmTarget = "$javaTarget"
            kotlinOptions.freeCompilerArgs += "-Xjdk-release=$javaTarget"
        }
    }

    js(IR) {
        browser {
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

                implementation(libs.adventure.minimessage)
                implementation(libs.adventure.text.serializer.gson)
                implementation(libs.cache4k)
                implementation(libs.logback.classic)
            }
        }
    }
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}

jib {
    to.image = "ghcr.io/kyoripowered/adventure-webui/webui"
    from {
        image = "eclipse-temurin:$javaTarget-jre"
        platforms {
            // We can only build multi-arch images when pushing to a registry, not when building locally
            val requestedTasks = gradle.startParameter.taskNames
            if ("jibBuildTar" in requestedTasks || "jibDockerBuild" in requestedTasks) {
                platform {
                    // todo: better logic
                    architecture = when (System.getProperty("os.arch")) {
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
        setMainClass(application.mainClass)
    }
}

tasks {
    val webpackTask = if (isDevelopment()) {
        "jsBrowserDevelopmentWebpack"
    } else {
        "jsBrowserProductionWebpack"
    }.let { taskName ->
        named<KotlinWebpack>(taskName)
    }

    named<Jar>("jvmJar") {
        rootProject.indraGit.applyVcsInformationToManifest(manifest)
    }

    named<JavaExec>("run") {
        if (isDevelopment()) {
            jvmArgs("-Dio.ktor.development=true")
        }

        classpath(named<Jar>("jvmJar"))
    }

    named<AbstractCopyTask>("jvmProcessResources") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(webpackTask.map { File(it.destinationDirectory, it.outputFileName) })
        filesMatching("application.conf") {
            expand(
                "jsScriptFile" to "${rootProject.name}.js",
                "miniMessageVersion" to libs.adventure.minimessage.get().versionConstraint.requiredVersion,
                "commitHash" to rootProject.extensions.getByType<IndraGitExtension>().commit()?.name.orEmpty(),
            )
        }
    }

    // Implicit task dependency issue?
    distTar {
        dependsOn("allMetadataJar", "jsJar")
    }

    distZip {
        dependsOn("allMetadataJar", "jsJar")
    }
}

/** Checks if the development property is set. */
fun isDevelopment(): Boolean = project.hasProperty("isDevelopment")
