# Hypixel Autocomplete

Tab-complete player names in Hypixel commands such as `/msg` and `/party`. The names come from your guild and from the people you message and party with.

[Modrinth](https://modrinth.com/mod/hypixel-autocomplete) · [sk1er.club](https://sk1er.club/mods/hypixel_auto_complete)

## Features

- **Guild members.** When the game starts, the mod fetches your guild roster and suggests those names.
- **People you talk to.** The mod remembers names you use in whisper and party commands. It saves them with their proper Minecraft capitalization, which it looks up from Mojang's API.
- **Party subcommands.** `/p` and `/party` complete subcommands like `invite`, `kick`, `promote`, `warp` and `list`, then the player name.
- **Hypixel only.** Names are only recorded from commands you send on Hypixel. Nothing is recorded on other servers or in singleplayer.

### Supported commands

| Kind | Commands |
| --- | --- |
| Whisper | `/msg`, `/tell`, `/w`, `/t`, `/whisper`, `/boop` |
| Party | `/p`, `/party` with `invite`, `challenge`, `promote`, `remove`, `kick`, `accept`, `kickoffline`, `leave`, `disband`, `private`, `home`, `warp`, `list`, `mute` |

### `/delname`

`/delname <name> [name...]` removes one or more names from the remembered history, and it tab-completes them too. The history is stored in `config/hypixel-autocomplete/history.json`.

## Supported versions

Fabric, for Minecraft 1.21.5, 1.21.6, 1.21.7, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.2 and 26.3.

Fabric API is not needed as a separate install: the jar bundles the Fabric API modules it uses.

## Building

The project uses [Essential's multi-version Gradle toolkit](https://github.com/EssentialGG/essential-gradle-toolkit). One source tree is preprocessed for each Minecraft version in `versions/`.

```sh
./gradlew build                  # every version
./gradlew :26.3-fabric:build     # a single version
./gradlew test                   # unit tests
```

Jars are written to `versions/<version>/build/libs/`. The 26.x targets need Java 25.

## License

[GPL-3.0](LICENSE)
