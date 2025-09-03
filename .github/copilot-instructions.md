# FramelessViewer - Image Viewer with Tiling

FramelessViewer is a Kotlin-based Swing GUI application that provides image viewing capabilities with tiling functionality. The application supports various image formats including WebP and HEIF, and can run in both GUI and daemon modes.

**Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

## Working Effectively

### Bootstrap and Setup

**CRITICAL**: This project requires Java 22. The GitHub Actions CI currently uses Java 17 but this will cause build failures.

Install Java 22 (REQUIRED):
```bash
# Download and install OpenJDK 22
cd /tmp
wget https://github.com/adoptium/temurin22-binaries/releases/download/jdk-22.0.2%2B9/OpenJDK22U-jdk_x64_linux_hotspot_22.0.2_9.tar.gz
tar -xzf OpenJDK22U-jdk_x64_linux_hotspot_22.0.2_9.tar.gz
sudo mkdir -p /usr/lib/jvm/temurin-22-jdk-amd64
sudo mv jdk-22.0.2+9/* /usr/lib/jvm/temurin-22-jdk-amd64/
```

Verify Java 22 installation:
```bash
ls /usr/lib/jvm/ | grep temurin-22
```

### Build Process

Build the project:
```bash
./gradlew build
```
- Build time: ~5 seconds. NEVER CANCEL. Set timeout to 60+ seconds.

Build distribution JAR:
```bash
./gradlew shadowJar
```
- Build time: ~3 seconds. NEVER CANCEL. Set timeout to 30+ seconds.

Full build sequence:
```bash
./gradlew clean build shadowJar
```
- Total time: ~5 seconds. NEVER CANCEL. Set timeout to 60+ seconds.

Run checks (includes compilation and basic validation):
```bash
./gradlew check
```
- Check time: ~1 second. NEVER CANCEL. Set timeout to 30+ seconds.

Clean build artifacts:
```bash
./gradlew clean
```

### Running the Application

**Note**: This is a GUI application that requires X11 display. In headless environments, you can only test command-line functionality.

Show help information:
```bash
# Use Java 22 explicitly since application is compiled with Java 22
/usr/lib/jvm/temurin-22-jdk-amd64/bin/java -jar build/libs/FramelessViewer-latest-all.jar --help
```

Run in daemon mode (better for headless testing):
```bash
/usr/lib/jvm/temurin-22-jdk-amd64/bin/java -jar build/libs/FramelessViewer-latest-all.jar --daemon
```

Expected command-line options:
- `-d, --daemon`: Start with daemon mode
- `-h, --help`: Show help message
- `-p, --path=<initPath>`: Path of the image to be loaded from the beginning
- `-V, --version`: Print version information
- `dc`: Daemon Client Subcommands (when daemon mode is enabled)

## Validation

### After Making Changes
Always run these validation steps in order:

1. **Build Validation**:
   ```bash
   ./gradlew clean build shadowJar
   ```
   - Full build should complete in ~5 seconds total
   - Check for any compilation errors

2. **Basic Functionality Test**:
   ```bash
   /usr/lib/jvm/temurin-22-jdk-amd64/bin/java -jar build/libs/FramelessViewer-latest-all.jar --help
   ```
   - Should display help message without errors
   - Should show version, options, and commands

3. **Runtime Validation**:
   ```bash
   timeout 10 /usr/lib/jvm/temurin-22-jdk-amd64/bin/java -jar build/libs/FramelessViewer-latest-all.jar --daemon
   ```
   - Should start daemon mode and log initialization
   - HeadlessException is expected in non-GUI environments

### Manual UI Testing (when GUI available)
- Test image loading with different formats (JPEG, PNG, WebP, HEIF)
- Test tiling functionality
- Test keyboard shortcuts (reference `example_of_keymaps-overrides.yml`)
- Test popup menu functionality

### No Automated Tests
**Important**: This project has no actual test suite. The `./gradlew test` command runs but there are no test cases defined. Do not rely on automated testing for validation.

## Project Structure

### Key Source Files
```
src/main/kotlin/me/bluegecko/framelessviewer/
├── Main.kt              # Application entry point, logging setup
├── App.kt               # Main application window and UI logic
├── AppController.kt     # Application lifecycle and thread management
├── AppKeymaps.kt        # Keyboard shortcut handling
├── ArgumentsParser.kt   # Command-line argument parsing (PicoCLI)
├── Daemon.kt           # Daemon mode implementation
├── ImagePanel.kt       # Image display and tiling logic
├── PopupMenu.kt        # Context menu functionality
├── data/               # Data classes and models
│   ├── AppData.kt
│   ├── ImagePanelData.kt
│   ├── KeyData.kt
│   └── ThreadData.kt
└── window/             # Additional UI windows
    └── KeybindingWindow.kt
```

### Configuration Files
- `build.gradle.kts`: Main build configuration (Kotlin DSL)
- `settings.gradle.kts`: Gradle settings
- `gradle.properties`: Kotlin code style configuration
- `example_of_keymaps-overrides.yml`: Example user keymap configuration

### Build Artifacts
- `build/libs/FramelessViewer-latest.jar`: Basic JAR (requires classpath)
- `build/libs/FramelessViewer-latest-all.jar`: Fat JAR with all dependencies (use this)

### CI Configuration
- `.github/workflows/main.yml`: GitHub Actions workflow
  - **CRITICAL BUG**: Currently uses Java 17 but project requires Java 22
  - **ALL CI BUILDS WILL FAIL** until line 21 is changed from `java-version: "17"` to `java-version: "22"`
  - This affects all pull requests and pushes to master branch

## Common Development Tasks

### Adding Dependencies
Edit `build.gradle.kts` and add to the `dependencies` block:
```kotlin
implementation("group:artifact:version")
```

### Configuration Changes
- User keymaps: `~/.framelessviewer/keymaps-overrides.yml`
- Application logs: `~/.framelessviewer/latest.log`

### Troubleshooting Build Issues
1. **Java Version Mismatch**: 
   - Ensure Java 22 is installed and detected by Gradle
   - Check with `ls /usr/lib/jvm/ | grep temurin-22`
   - If missing, follow Java 22 installation steps above
2. **UnsupportedClassVersionError**: 
   - Application was compiled with Java 22 but running with older Java
   - Use `/usr/lib/jvm/temurin-22-jdk-amd64/bin/java` explicitly
3. **Dependency Resolution**: Run `./gradlew --refresh-dependencies`
4. **Clean Build**: Run `./gradlew clean build`
5. **CI Failures**: Update `.github/workflows/main.yml` to use Java 22

### Known Issues and Limitations
- **No automated test coverage** - validation must be done manually
- **GUI requires X11** - only command-line interface works in headless environments
- **Java 22 mandatory** - will not compile or run with earlier versions
- **CI configuration broken** - uses Java 17, must be updated to Java 22
- **HeadlessException expected** - normal behavior in non-GUI environments when GUI is initialized

## Working with Images
The application supports these formats:
- Standard formats: JPEG, PNG, GIF, BMP
- WebP: Via TwelveMonkeys ImageIO
- HEIF: Via nightmonkeys library (runtime only)

Configuration directory: `$HOME/.framelessviewer/`
- `keymaps-overrides.yml`: Custom keyboard shortcuts
- `latest.log`: Application log file