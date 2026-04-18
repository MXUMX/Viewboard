# ViewBoard

[<img src="https://shields.io" alt="Modrinth" />](https://modrinth.com/mod/viewboard)

[<img src="https://shields.io" alt="CurseForge" />](https://www.curseforge.com/minecraft/mc-mods/viewboard)

ViewBoard is a client-side NeoForge mod for Minecraft 1.21.11 that adds a keyboard map screen to the controls menu. The screen helps show which keys are used, conflicting, or still free.

This branch (`neoforge-1.21.11`) has been tested and confirmed to work only with Minecraft `1.21.11` on NeoForge `21.11.42`.

IMPORTANT NOTE: A GUI scale of ***3 or below*** is required to properly display the keyboard.

## Requirements

- Java 21
- Minecraft 1.21.11
- NeoForge 21.11.42

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
viewboard-neoforge-1.21.11-1.0.1.jar
viewboard-neoforge-1.21.11-1.0.1-sources.jar
```

## Project Layout

- `src/main/java` contains the mod source code
- `src/main/resources` contains metadata and assets
- `build.gradle` and `gradle.properties` define the NeoForge build

## Mod ID

The mod ID is `viewboard`.

## License

This project is licensed under the MIT License. See `LICENSE` for details.
