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
https://philipp0205.github.io/eclipse-icon-plugin/p2/
```

The `/p2/` suffix is required. The project's GitHub Pages root serves a landing
page, not p2 metadata, so Eclipse cannot resolve it as a repository.

The site lives in the `p2/` directory of `main` and is refreshed by the release
workflow. To test an unreleased change, download the `p2-update-site` artifact
from that revision's workflow run and add the extracted folder as a local
repository.

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

The update site is produced at `repository/target/repository/`. Add that folder
in Eclipse as a local repository to test it before publishing.

Releases are published by pushing a `v*` tag, which copies the freshly built
repository into `p2/` on `main`. GitHub Pages then serves it.

## License

MIT — see [LICENSE](LICENSE).
