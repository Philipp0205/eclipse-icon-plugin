
# Per-Workspace Eclipse Icon Plugin

A lightweight Eclipse plugin that allows you to distinguish multiple Eclipse workspaces by setting unique taskbar/window icons and title suffixes. Each workspace can have its own icon configuration.

## Features

- **Workspace-Based Icons**: Set unique icons for each workspace, visible in taskbar, alt-tab switcher, and window decorations
- **Title Suffix**: Append a custom suffix to window titles (e.g., `[DEV]`, `[PROD]`)
- **Per-Workspace Settings**: Icon preferences are stored per workspace, so different workspaces automatically show different icons
- **Multiple Configuration Sources**: System properties, environment variables, and preferences
- **Cross-Platform**: Works on Linux (KDE Plasma, GNOME, etc.) with both X11 and Wayland
- **PDE Support**: Works in both host IDE and PDE-launched target Eclipse instances
- **Early Startup**: Applies icons as soon as workbench windows open

## Requirements

- Eclipse 4.x+ (2024-xx or later recommended)
- Java 17 or later
- Linux with X11 or Wayland (tested on KDE Plasma 5/6, RHEL9)

## Installation

### Option 1: Dropins (Simplest)

1. Build the plugin JAR (or download from releases)
2. Copy the JAR to `ECLIPSE_HOME/dropins/`
3. Restart Eclipse

### Option 2: Install New Software

1. Add the update site URL in Eclipse: `Help > Install New Software...`
2. Select the "Per-Instance Eclipse Icon" plugin
3. Complete the installation and restart

## Configuration

Configuration sources are checked in the following precedence order:

1. **System properties** (highest priority)
2. **Environment variables**
3. **Plugin preferences**
4. **Fallback** (embedded default icon)

### System Properties

Add to `eclipse.ini` (after `-vmargs`) or pass on command line:

```
-Declipse.instance.icon=/path/to/your/icon.png
-Declipse.instance.titleSuffix=[DEV]
```

### Environment Variables

Set before launching Eclipse:

```bash
export ECLIPSE_INSTANCE_ICON=/path/to/your/icon.png
export ECLIPSE_INSTANCE_TITLE_SUFFIX=[DEV]
/opt/eclipse/eclipse
```

### Plugin Preferences

1. Go to `Window > Preferences > Instance Icon`
2. Choose from predefined icons or set a custom icon file path
3. Optionally set a title suffix
4. Click Apply and OK

**Note**: Settings are stored per workspace. When you open a different workspace, that workspace will have its own independent icon settings. This allows you to easily distinguish between workspaces (e.g., DEV, PROD, TEST) without needing launcher scripts or environment variables.

#### Predefined Icons

The plugin includes the following bundled icons:

| Name | Description |
|------|-------------|
| Original (Purple) | Default Eclipse purple color |
| Blue | Blue-themed Eclipse icon |
| Sky | Light blue/sky-themed icon |
| Green | Green-themed Eclipse icon |
| Sage | Sage green-themed icon |
| Red | Red-themed Eclipse icon |
| Rose | Rose/pink-themed icon |

**Note**: System properties and environment variables take precedence over preferences.

## Usage Examples

### Running Multiple Instances with Different Icons

Create a launcher script for each instance:

**eclipse-dev.sh:**
```bash
#!/bin/bash
export ECLIPSE_INSTANCE_ICON=/home/user/icons/eclipse-dev.png
export ECLIPSE_INSTANCE_TITLE_SUFFIX="[DEV]"
/opt/eclipse/eclipse
```

**eclipse-prod.sh:**
```bash
#!/bin/bash
export ECLIPSE_INSTANCE_ICON=/home/user/icons/eclipse-prod.png
export ECLIPSE_INSTANCE_TITLE_SUFFIX="[PROD]"
/opt/eclipse/eclipse
```

### KDE Desktop Entry Override

Create a user-local copy in `~/.local/share/applications/eclipse-dev.desktop`:

```desktop
[Desktop Entry]
Type=Application
Name=Eclipse (DEV)
Exec=/opt/eclipse/eclipse -vmargs -Declipse.instance.icon=/home/user/icons/eclipse-dev.png -Declipse.instance.titleSuffix=[DEV]
Icon=/home/user/icons/eclipse-dev.png
Categories=Development;IDE;
```

### PDE Launch Configuration

1. Open `Run > Run Configurations...`
2. Select your Eclipse Application configuration
3. Go to `Arguments` tab
4. Add to VM arguments:
   ```
   -Declipse.instance.icon=/path/to/icon.png
   -Declipse.instance.titleSuffix=[TARGET]
   ```

Or use the Environment tab to set `ECLIPSE_INSTANCE_ICON` and `ECLIPSE_INSTANCE_TITLE_SUFFIX`.

## Icon Requirements

### Recommended Format

- **Format**: PNG (24-bit with alpha transparency)
- **Sizes**: Provide multiple sizes for best quality at various DPIs:
  - 16x16 (small icons)
  - 24x24 (panels)
  - 32x32 (menus)
  - 48x48 (taskbar)
  - 64x64 (alt-tab)
  - 128x128 (high-DPI displays)

### Single File Approach

You can provide a single high-resolution PNG (e.g., 256x256 or 512x512) and the plugin will scale it automatically. However, pre-scaled icons at standard sizes will look sharper.

### Icon Naming Convention

For a set of sized icons:
```
icons/
├── eclipse-dev-16.png
├── eclipse-dev-24.png
├── eclipse-dev-32.png
├── eclipse-dev-48.png
├── eclipse-dev-64.png
└── eclipse-dev-128.png
```

When using a single file, just provide the path to the largest icon:
```
/home/user/icons/eclipse-dev-128.png
```

## Troubleshooting

### Icons Not Appearing

1. **Check file path**: Ensure the path is absolute and the file exists
2. **Check permissions**: The icon file must be readable by the user running Eclipse
3. **Check format**: Only PNG files are supported
4. **Check logs**: View `Error Log` view for warnings/errors from the plugin

### Stale Icons in Taskbar (KDE)

KDE may cache icons. Try:
```bash
kbuildsycoca5 --noincremental
```
Or log out and log in again.

### HiDPI Displays

Provide multiple icon sizes or a large source icon (128x128 or larger). The plugin will scale as needed.

### Flatpak/Snap Sandboxing

Icon files outside the sandbox may not be accessible. Place icons in allowed paths or within the home directory.

### Wayland Considerations

Some Wayland compositors may have limitations with per-window icon changes. The title suffix feature helps distinguish instances when icons are not fully supported.

## Building from Source

### Prerequisites

- Eclipse IDE for RCP/Plugin Development
- Java 17 SDK

### Build Steps

1. Import the project: `File > Import > Existing Projects into Workspace`
2. Ensure target platform includes `org.eclipse.ui` and `org.eclipse.core.runtime`
3. Export: `File > Export > Plug-in Development > Deployable plug-ins and fragments`
4. Select the plugin and export as a JAR

### Optional: Tycho Build

For CI/CD, you can set up a Tycho build. See Eclipse Tycho documentation.

## API

The plugin does not expose public API. All functionality is internal and activated automatically at startup.

## License

This plugin is provided as-is. Feel free to modify and distribute.

## Changelog

### 1.0.0

- Initial release
- Custom icon support (PNG)
- Title suffix support
- Configuration via system properties, environment variables, and preferences
- Preference page with icon preview
- Fallback icon when no custom icon is configured
