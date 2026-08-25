# Per-Workspace Eclipse Icon

An Eclipse plug-in that gives each workspace a distinct window/taskbar icon,
short icon label, and title suffix.

## Requirements

- Eclipse 2026-06 or newer
- Java 21

Window icons are applied through SWT. Desktop environments, especially Wayland
compositors, may choose not to display per-window icons; the title suffix remains
available in that case.

## Install

In Eclipse, open **Help → Install New Software…**, add this update site, and
select **Per-Workspace Eclipse Icon**:

```text
https://philipp0205.github.io/eclipse-icon-plugin/
```

The site is published from `main` by GitHub Actions. For a pull-request test
build, download the `p2-update-site` workflow artifact or use the temporary raw
GitHub URL supplied in that pull request.

Restart Eclipse after the initial installation. Configure the workspace under
**Window → Preferences → General → Workspace Icon**. Changes made with Apply or
Apply and Close take effect immediately.

## Configuration

Preferences are stored in the workspace metadata, independently for every
workspace. The page supports:

- generated icon colors;
- bundled purple, blue, sky, green, sage, red, and rose icons;
- an external PNG;
- an icon label of up to four characters;
- label color and size;
- a window-title suffix such as `[DEV]`;
- temporarily disabling and restoring the original window appearance.

Launcher configuration overrides workspace preferences:

```text
-Declipse.instance.icon=/absolute/path/icon.png
-Declipse.instance.titleSuffix=[DEV]
```

Equivalent environment variables:

```text
ECLIPSE_INSTANCE_ICON=/absolute/path/icon.png
ECLIPSE_INSTANCE_TITLE_SUFFIX=[DEV]
```

Precedence is system property, environment variable, then workspace preference.

## Build

The Maven/Tycho reactor builds the plug-in, installable feature, and p2 site:

```bash
./mvnw clean verify
```

The update site is produced at `repository/target/repository/`.

## License

MIT — see [LICENSE](LICENSE).
