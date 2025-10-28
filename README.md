# FramelessViewer

A quirky image viewer built with Kotlin and Swing, featuring tiling, title bar switching, and WebP support

> [!CAUTION]
> This repository is largely unmaintained.
> There are no plans for new feature additions or bug fixes.

> [!WARNING]
> This was created as a hobby for study purposes, so it is not at a practical level.

## Features

- **Multi-format Support**: View images in standard formats (JPEG, PNG, GIF, BMP) as well as modern formats (like WEBP, HEIF, JPEG XL, AVIF)
- **Tiling Functionality**: Display and organize multiple images with tiling capabilities
- **Cross-platform**: Runs on any system with Java 22+
- **Cross-window Transfer**: Transfer the open image to another window

## Requirements

- Java 22 or higher

## Installation

### Building from Source

1. Clone the repository:

```bash
git clone https://github.com/BlueGeckoJP/FramelessViewer.git
cd FramelessViewer
```

1. Build the project:

```bash
./gradlew clean build shadowJar
```

The executable JAR will be created at `build/libs/FramelessViewer-latest-all.jar`

## Usage

### Basic Usage

Run the application with:

```bash
java -jar build/libs/FramelessViewer-latest-all.jar
```

### Command-line Options

- `-d, --daemon` - Start in daemon mode
- `-h, --help` - Display help information
- `-p, --path=<path>` - Open a specific image at startup
- `-V, --version` - Display version information
- `dc open <path>` - Open a specific image in the daemon mode window

### Opening an Image

```bash
java -jar build/libs/FramelessViewer-latest-all.jar --path /path/to/image.jpg
```

### Daemon Mode

Start the daemon:

```bash
java -jar build/libs/FramelessViewer-latest-all.jar --daemon
```

Open an image via daemon client:

```bash
java -jar build/libs/FramelessViewer-latest-all.jar dc open /path/to/image.jpg
```

## Configuration

### Custom Keybindings

Create a configuration file at `~/.framelessviewer/keymaps-overrides.yml`:

```yaml
halfOfAllLeft:
  keyCode: 37
  ctrl: true
  shift: true
  alt: true
```

See `example_of_keymaps-overrides.yml` for more examples.

### Application Files

- Configuration directory: `~/.framelessviewer/`
- Log file: `~/.framelessviewer/latest.log`
- Custom keymaps: `~/.framelessviewer/keymaps-overrides.yml`

## Development

### Project Structure

```txt
src/main/kotlin/me/bluegecko/framelessviewer/
├── Main.kt              # Application entry point
├── App.kt               # Main application window
├── AppController.kt     # Application lifecycle management
├── AppKeymaps.kt        # Keyboard shortcut handling
├── ArgumentsParser.kt   # Command-line argument parsing
├── Daemon.kt           # Daemon mode implementation
├── ImagePanel.kt       # Image display and tiling logic
├── PopupMenu.kt        # Context menu functionality
├── data/               # Data classes and models
└── window/             # Additional UI windows
```

### Technology Stack

- **Language**: Kotlin 2.1.21
- **UI Framework**: Java Swing with FlatLaf themes
- **Image Processing**: TwelveMonkeys ImageIO, nightmonkeys
- **CLI**: PicoCLI
- **Concurrency**: Kotlinx Coroutines
- **Logging**: SLF4J with Logback
- **Configuration**: SnakeYAML
- **Build Tool**: Gradle with Shadow plugin

### Building

```bash
# Clean and build
./gradlew clean build

# Create fat JAR
./gradlew shadowJar
```

### Running in Development

```bash
./gradlew run
```

## Supported Image Formats

- **Standard**: JPEG, PNG, GIF, BMP
- **WebP**: Via TwelveMonkeys ImageIO plugin
- **HEIF/HEIC/AVIF/JPEG XL**: Via nightmonkeys library (runtime)

## Troubleshooting

### Java Version Issues

If you encounter `UnsupportedClassVersionError`, ensure you're using Java 22:

```bash
java -version
```

### Logs

Check the log file for errors and debugging information:

```bash
cat ~/.framelessviewer/latest.log
```

## Links

- **Repository**: <https://github.com/BlueGeckoJP/FramelessViewer>
- **Issues**: <https://github.com/BlueGeckoJP/FramelessViewer/issues>

## One final remark

余計に触りたくないリポジトリランキング第1位おめでとうございます!!
