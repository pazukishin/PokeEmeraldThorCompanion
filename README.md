# Thor Companion

**Disclaimer**: This project was entirely developed with the assistance of AI.

Companion Android application for playing Pokemon Emerald on RetroArch from an Ayn Thor.

## Features

- Compact horizontal screen, designed for the 3.92-inch secondary display
- UDP connection with short timeout to avoid blocking gameplay experience
- Initial memory reading using `READ_CORE_MEMORY`
- Current map and encounter list with level, method, and capture rate
- Online query to PokéAPI for Emerald encounters
- Local catalog as backup for offline use

## System Requirements

### Pokemon Emerald ROM
The application is specifically configured to work with **Pokemon Emerald Version 1.0 (USA)** ROM, which is the version used in RetroAchievements. This ROM must be compatible with the RetroArch V.GBA-Next core for proper functionality.

### RetroArch Core
The target core is **V.GBA-Next**. This core is necessary to correctly read memory data from the Pokemon Emerald USA v1.0 ROM. The core must be configured with the following characteristics:
- Compatible with Pokemon Emerald Version 1.0 (USA)
- Memory reading enabled in Network Control Interface

## How to Run

Open the project in Android Studio and run the `app` configuration with Android SDK 35.
The device must be able to reach the Ayn Thor's IP address. In RetroArch, you need to enable
`Settings > Network > Network Control Interface` and configure the UDP port used by the app.

## Memory Data Dependencies

`RetroArchClient.kt` contains the command and memory address. The address
`0x020322E4` is configured for Pokemon Emerald USA v1.0 using V.GBA-Next and reads as two bytes: group and map number. The profile is isolated in `CoreProfiles`
to add ROM revisions without touching the UI. This configuration matches the
USA ROM used with RetroAchievements. The Network Control Interface must also
allow memory reading.

## Technical Details

The application reads memory from RetroArch using the READ_CORE_MEMORY command to get current map information. It uses a specific memory address offset (0x020322E4) that is valid for Pokemon Emerald USA v1.0 with V.GBA-Next core.

The app provides:
- Current map display
- Encounter information including species, levels, and capture rates
- Trainer data with Pokémon parties
- Trade information between Pokémon species

## Next Recommended Iteration

Create a table of offsets by ROM region/version, persist host and port settings, and add
a configuration screen to select the profile. It's also recommended to cache the
PokéAPI JSON in local storage so new areas remain available offline.