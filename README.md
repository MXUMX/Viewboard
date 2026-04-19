# ViewBoard

## Port Status

This branch (`codex/fabric-1.20.1-port`) is an attempted Fabric 1.20.1 port.

Critical blocker:
- The current implementation relies on NeoForge-only client controls APIs that do not exist on Fabric in the current single-source layout. The failed Fabric compile is blocked by:
- `net.neoforged.neoforge.client.settings.KeyModifier` and NeoForge keybinding methods such as `setKeyModifierAndCode(...)` / `hasKeyModifierConflict(...)`
- NeoForge screen lifecycle hooks in `ViewBoardClientEvents`
- NeoForge-specific controls screen classes/package paths used by the controls integration bridge

What this means:
- A full-parity Fabric 1.20.1 port requires a dedicated platform compatibility layer or split source set, not just dependency/version changes.
- The attempted Fabric build currently fails at compile time for those platform-specific APIs.

[<img src="https://img.shields.io/modrinth/dt/xf9PORQx?logo=modrinth&style=for-the-badge" alt="Modrinth Downloads" />](https://modrinth.com/mod/viewboard)

[<img src="https://img.shields.io/curseforge/dt/1485396?logo=curseforge&style=for-the-badge" alt="CurseForge Downloads" />](https://www.curseforge.com/minecraft/mc-mods/viewboard)

ViewBoard is a client-side NeoForge mod for Minecraft 1.21.1 that adds a keyboard map screen and keybind-management tools to the controls menu. The screens help show which keys are used, conflicting, ignored, grouped, or still free.

This branch (`neoforge-1.21.1-1.21.5`) targets Minecraft `1.21.1` on NeoForge `21.1.172`.

ViewBoard supports most GUI scales, but the keyboard view is currently most comfortable at GUI scale `3` or below.

## Requirements

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.172

## New Features

- Support for most GUI scales, with the best results at GUI scale `3` or below
- An ignore system for keybinds that lets intentional conflicts exist without warnings
- Vanilla Controls / Key Binds integration that respects ignored keybinds and suppresses those warnings there too
- A keybind group system where grouped keybinds share a forced key until they are removed from the group
- Multiple keyboard layouts in the keyboard view: `QWERTY`, `AZERTY`, and `QWERTZ`
- Mouse button support in the keyboard view
- Detection and display of custom keys assigned through Minecraft's keybind settings

## Usage

1. Open Minecraft's `Controls` / `Key Binds` screen.
2. Use the `Keyboard View` button in the bottom-right corner to open the keyboard map.
3. Hover keys in the keyboard view to inspect conflicts, ignored bindings, and grouped bindings.
4. Use `Manage Rules` to open the Keybind Rules screen.
5. In `Keybind Rules`, search keybinds, toggle whether a binding is ignored, and jump into group editing for a specific keybind.
6. In `Keybind Groups`, create a group, rename it, assign a shared trigger key, and add or remove bindings from that group.
7. When a binding is marked as ignored, ViewBoard and the vanilla keybind screen both stop warning about that intentional conflict.

## Build

Use the Gradle wrapper from the project root:

```bash
./gradlew clean build
```

Built jars are written to:

```text
build/libs/
```

The build produces both the mod jar and a matching `-sources` jar. The generated filenames follow this pattern:

```text
viewboard-neoforge-<minecraft-version>-<mod-version>.jar
viewboard-neoforge-<minecraft-version>-<mod-version>-sources.jar
```

For example:

```text
viewboard-neoforge-1.21.1-1.1.0.jar
viewboard-neoforge-1.21.1-1.1.0-sources.jar
```

## Project Layout

- `src/main/java` contains the mod source code
- `src/main/resources` contains metadata and assets
- `build.gradle` and `gradle.properties` define the NeoForge build

## Mod ID

The mod ID is `viewboard`.

## License

This project is licensed under the MIT License. See `LICENSE` for details.
