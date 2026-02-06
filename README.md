# world-portal

Basic JavaFX scaffold for a future desktop tool that transfers game worlds between a local installation and an SSH server.

This repository currently contains structure only:
- GUI scaffolding (FXML + controller)
- Model and service interfaces
- Stub implementations with no transfer logic

## Tech Stack
- Java 23+
- Gradle
- JavaFX 25
- JSch 2.27.2

## Run
If Gradle wrapper is present:

```bash
./gradlew run
```

Or with local Gradle:

```bash
gradle run
```

## Build and Test

```bash
./gradlew build
```

## Package for Distribution

Create a platform-specific app image on the current OS:

```bash
./gradlew packageAppImage -PjpackageType=app-image
```

Output is written to:

```text
build/jpackage/out
```

## GitHub Actions Release

This repository includes a release workflow at `.github/workflows/release.yml`:
- Builds on Linux, macOS, and Windows
- Runs tests
- Packages with `jpackage`
- Uploads artifacts for each OS
- Publishes a GitHub Release automatically when pushing a `v*` tag
