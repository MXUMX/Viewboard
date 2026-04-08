# ViewBoard

ViewBoard is a client-side NeoForge mod for Minecraft 1.21.1 that adds a keyboard map screen to the controls menu. The screen helps show which keys are used, conflicting, or still free.

This branch (`neoforge-1.21.1-1.21.5`) has been tested and confirmed to work on Minecraft `1.21.1` through `1.21.5` with NeoForge `21.1.172`.

IMPORTANT NOTE: A GUI scale of ***3 or below*** is required to properly display the keyboard.

## Requirements

- Java 21
- Minecraft 1.21.1 through 1.21.5
- NeoForge 21.1.172

## Build

Use the Gradle wrapper from the project root:

```bash
./gradlew build
```

Built jars are written to:

```text
build/libs/
```

The standard build now produces both the mod jar and a matching `-sources` jar.

## Project Layout

- `src/main/java` contains the mod source code
- `src/main/resources` contains metadata and assets
- `build.gradle` and `gradle.properties` define the NeoForge build

## Mod ID

The mod ID is `viewboard`.

## License

This project is licensed under the MIT License. See `LICENSE` for details.
