# ViewBoard

[<img src="https://img.shields.io/modrinth/dt/xf9PORQx?logo=modrinth&style=for-the-badge" alt="Modrinth Downloads" />](https://modrinth.com/mod/viewboard)

[<img src="https://img.shields.io/curseforge/dt/1485396?logo=curseforge&style=for-the-badge" alt="CurseForge Downloads" />](https://www.curseforge.com/minecraft/mc-mods/viewboard)

ViewBoard is a client-side NeoForge mod that adds a keyboard map and keybind-management tools to Minecraft's controls menu. It helps show which keys are used, conflicting, ignored, grouped, or still free.

ViewBoard supports most GUI scales, but the keyboard view is currently most comfortable at GUI scale `3` or below.

## Requirements

- NeoForge
- A supported Minecraft version for the branch or release you are using
- The Java version required by that Minecraft/NeoForge target

## Feature List

### Core Keyboard View

- Adds a `Keyboard View` button to Minecraft's Controls / Key Binds screen.
- Shows a visual keyboard map with used, conflicting, and unused keys.
- Supports multiple keyboard layouts: `QWERTY`, `AZERTY`, and `QWERTZ`.
- Shows mouse buttons and custom keys assigned through Minecraft's keybind settings.
- Provides hover tooltips that list the keybinds using each key.

### Keybind Rules and Conflict Management

- Adds an ignore system for intentional keybind conflicts.
- Suppresses ignored conflicts in ViewBoard's keyboard view.
- Integrates with the vanilla Controls / Key Binds screen so ignored conflicts do not show as warnings there.
- Adds a Keybind Rules screen with search and filters for active or ignored bindings.

### Keybind Groups

- Adds keybind groups for bindings that should intentionally share one trigger.
- Lets grouped keybinds use a shared key while preserving their original binding for restoration.
- Provides a group editor for creating, renaming, assigning, removing, and deleting groups.
- Excludes same-group bindings from effective conflict summaries.

### Controls Screen Layout

- Adds a saved Controls / Key Binds list-width option.
- Width modes are `Default`, `Large`, and `Very Large`.
- The wider modes help long keybind names and modded controls fit without overlapping ViewBoard's row tools.

### Compatibility

- Supports Controlling's replacement keybind screen, including ViewBoard ignore/group conflict rules, conflict filtering, and keybind tooltips.
- Avoids FancyMenu widget-discovery slowdowns by rendering per-row ViewBoard tools without registering hundreds of extra screen widgets.
- Works alongside KeyBind Bundles by skipping its non-key bundle control row while treating real bundle keybind rows like normal keybinds.

## Usage

1. Open Minecraft's `Controls` / `Key Binds` screen.
2. Use `Keyboard View` to open the keyboard map.
3. Hover keys in the keyboard view to inspect usage, conflicts, ignored bindings, and grouped bindings.
4. Use `Manage Rules` to search bindings, toggle ignored conflicts, or jump into group editing.
5. Use the row `G` control to edit groups for a keybind.
6. Use the row `I` control to toggle whether a keybind's conflict should be ignored.
7. Use the width button on the keybind screen to cycle between `Default`, `Large`, and `Very Large`.

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

## Project Layout

- `src/main/java` contains the mod source code
- `src/main/resources` contains metadata and assets
- `build.gradle` and `gradle.properties` define the NeoForge build

## Mod ID

The mod ID is `viewboard`.

## License

This project is licensed under the MIT License. See `LICENSE` for details.
