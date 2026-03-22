# StarLight Legacy

StarLight Legacy is a **Legacy Fabric 1.12.2 client mod** aimed at quick FPS gains by applying lightweight graphics defaults at client startup.

## Target platform
- Minecraft: `1.12.2`
- Loader: `legacy-fabric-loader`
- API: `legacy-fabric-api`

## What it does
On client initialization, the mod attempts to apply FPS-friendly settings such as:
- disabling fancy graphics and VSync
- lowering particles, clouds, and mipmaps
- reducing ambient occlusion and view distance
- capping FPS to a stable limit

The implementation uses reflective field lookup so it can gracefully skip unavailable settings without crashing, and attempts to save updated options.

## Configuration
A config file is created at `config/starlightlegacy.properties` with:
- `enabled=true` (set false to disable all tuning)
- `max_fps=120` (clamped to 30..260)
- `view_distance=8` (clamped to 2..16)

## Release file
The current root-level release file is `RELEASE.md`, and versioned release notes are kept in `releases/0.1.0.md`.

## Build
```bash
./gradlew build
```

## Run client in dev
```bash
./gradlew runClient
```
