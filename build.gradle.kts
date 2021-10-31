import net.kyori.indra.git.IndraGitExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    application
    id("com.diffplug.spotless")
    id("net.kyori.indra.git")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

spotless {
    kotlin {
        // spotless doesn't know about multi-platform source sets
        target(listOf("commonMain", "jsMain", "jvmMain")
            .map { sourceSet -> "src/$sourceSet/kotlin/**/*.kt" })

        // use dropbox style for 4 space indents
        ktfmt("0.27").dropboxStyle()
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

kotlin {
    explicitApi()

    jvm {
        withJava()
    }

    js {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        all {
            languageSettings {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.html)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.bundles.ktor.server)

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

tasks.getByName<Jar>("jvmJar") {
    val webpackTask = if (isDevelopment()) {
        "jsBrowserDevelopmentWebpack"
    } else {
        "jsBrowserProductionWebpack"
    }.let { taskName ->
        tasks.getByName<KotlinWebpack>(taskName)
    }

    dependsOn(tasks.getByName("check"), webpackTask)
    from(File(webpackTask.destinationDirectory, webpackTask.outputFileName))
    rootProject.extensions.findByType<IndraGitExtension>()!!.applyVcsInformationToManifest(manifest)
}

tasks.getByName<JavaExec>("run") {
    if (isDevelopment()) {
        jvmArgs("-Dio.ktor.development=true")
    }

    classpath(tasks.getByName<Jar>("jvmJar"))
}

tasks.getByName<AbstractCopyTask>("jvmProcessResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    filesMatching("application.conf") {
        expand("jsScriptFile" to "${rootProject.name}.js")
    }
}

/** Checks if the development property is set. */
fun isDevelopment(): Boolean = project.hasProperty("isDevelopment")
