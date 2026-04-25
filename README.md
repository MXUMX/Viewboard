# ViewBoard

## Port Status

This branch (`codex/neoforge-26.1.1-port`) is an attempted NeoForge `26.1.1` port.

Critical blocker:
- The Gradle/NeoForge toolchain could not resolve `net.neoforged:neoforge:26.1.1`.
- The attempted resolution URL was `https://maven.neoforged.net/releases/net/neoforged/neoforge/26.1.1/neoforge-26.1.1.pom`
- During configuration, the toolchain also failed to map `26.1.1` cleanly and reported:
- `Failed to parse MC version from NeoForge version 26.1.1. Using capabilities of latest known Minecraft version with Minecraft version 1.26.1.`

What this means:
- I could not complete a compile attempt for this target in the current environment because the requested upstream NeoForge artifact was unavailable and the version line could not be mapped cleanly by the tooling.



IMPORTANT PROBLEM: The last session on this branch was interrupted. So branch did not finish porting yet. 
Here is the logs:
```

Ran 2 commands
Ran git switch codex/neoforge-26.1.1-port
Ran git status --short --branch
The 26.1.1 branch is clean. I’m checking its declared Minecraft/NeoForge versions and README first, then I’ll verify against available NeoForge metadata before deciding whether this is a fixable branch or a documented impossible target.


Explored 4 files, 1 search, searched web 3 times
Read README.md
Read build.gradle
Read gradle.properties
Read neoforge.mods.toml
Searched for minecraft_version|neo_version|neoforge|26\.1\.1|1\.21\.1 in README.md
NeoForge 26.1.1 Minecraft version artifact net.neoforged neoforge 26.1.1
https://maven.neoforged.net/releases/net/neoforged/neoforge
'26.1.1' in https://maven.neoforged.net/releases/net/neoforged/neoforge
Research found the branch’s blocker: 26.1.1 is not a valid NeoForge artifact by itself. NeoForge’s 26.1+ versioning uses four components plus a beta suffix, and Maven currently has 26.1.1 builds such as 26.1.1.15-beta; I’m retargeting this branch to Minecraft 26.1.1 with that real NeoForge artifact.


Edited 3 files, ran 1 command
Edited build.gradle
Edited gradle.properties
Edited neoforge.mods.toml
Ran ./gradlew build
The 26.1.1 artifact is resolving now, which is good news. Gradle is generating the Minecraft/NeoForge workspace for actual 26.1.1, so the next output should be real API drift rather than dependency failure.

The 26.1.1 target is real, but it no longer supports Java 21: NeoForge/FML/NeoForm variants require Java 25. I’m checking the local JDKs now; if Java 25 exists, I’ll switch the toolchain and continue the compile.


Edited 1 file, ran 2 commands
Ran /usr/libexec/java_home -V
Edited build.gradle
Ran ./gradlew build
Worked for 22m 34s
Now we’re past dependency setup and into true 26.1.1 API changes. This is a larger Minecraft GUI rewrite: GuiGraphics became GuiGraphicsExtractor, input handlers now take event objects, and list entries use extractContent(...); I’m going to inspect the generated 26.1.1 sources/mappings and adapt the UI screens to that API rather than papering over it.

Explored 3 searches
Ran jar tf build/moddev/artifacts/minecraft-patched-26.1.1.15-beta-sources.jar | rg 'Screen.java|GuiGraphicsExtractor.java|ObjectSelectionList.java|ContainerObjectSelectionList.java|KeyBindsList.java|Button.java|KeyEvent.java|MouseButtonEvent.java' | sed -n '1,120p'

CRITICAL INTERRUPTION: {"type":"error","status":400,"error":{"type":"invalid_request_error","message":"The 'gpt-5.5' model is not supported when using Codex with a ChatGPT account."}}

```



[<img src="https://img.shields.io/modrinth/dt/xf9PORQx?logo=modrinth&style=for-the-badge" alt="Modrinth Downloads" />](https://modrinth.com/mod/viewboard)

[<img src="https://img.shields.io/curseforge/dt/1485396?logo=curseforge&style=for-the-badge" alt="CurseForge Downloads" />](https://www.curseforge.com/minecraft/mc-mods/viewboard)

ViewBoard is an attempted NeoForge `26.1.1` port.

This branch (`codex/neoforge-26.1.1-port`) is reserved for the NeoForge `26.1.1` port attempt.

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
