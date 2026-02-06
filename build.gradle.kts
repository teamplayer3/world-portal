plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.worldportal"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.mwiede:jsch:2.27.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

javafx {
    version = "25"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass = "io.worldportal.app.WorldPortalLauncher"
}

tasks.shadowJar {
    archiveClassifier.set("all")
}

tasks.register("packageAppImage") {
    group = "distribution"
    description = "Builds a platform-specific app image using jpackage."
    dependsOn(tasks.shadowJar)

    doLast {
        val shadowJarTask = tasks.shadowJar.get()
        val inputDir = layout.buildDirectory.dir("jpackage/input").get().asFile
        val outputDir = layout.buildDirectory.dir("jpackage/out").get().asFile
        val jarFile = shadowJarTask.archiveFile.get().asFile
        val appVersion = project.version.toString().substringBefore("-")
        val packageType = providers.gradleProperty("jpackageType").orElse("app-image").get()
        val osName = System.getProperty("os.name").lowercase()

        copy {
            from(jarFile)
            into(inputDir)
        }

        val command = mutableListOf(
            "jpackage",
            "--name", "world-portal",
            "--type", packageType,
            "--input", inputDir.absolutePath,
            "--main-jar", jarFile.name,
            "--main-class", application.mainClass.get(),
            "--app-version", appVersion,
            "--dest", outputDir.absolutePath,
            "--java-options", "-Dworldportal.debug=true",
            "--java-options", "-Dprism.verbose=true"
        )

        if (osName.contains("win")) {
            command.add("--win-menu")
            command.add("--win-shortcut")
            command.add("--win-dir-chooser")
            command.add("--win-per-user-install")
        }

        exec {
            commandLine(command)
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
