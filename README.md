# TheEscapists-J2ME

The Escapists fan demake for J2ME platform.

![screenshot](/screenshot.png)

## Project state

- NPC behavior is finished.
- Original maps parser is finished.
- Rendering engine is finished, but needs optimization.
- The game can be beaten with cheats.
- Some important mechanics are not yet implemented, e.g looting pockets.

See [TODO.md](/TODO.md) for more details

## Disclaimer

All rights to the original game belong to Mouldy Toof Studios and Team17.\
This repository does not contain any original game assets or decompiled code.

## Building

You have to obtain legal copy of The Escapists [(Steam)](https://store.steampowered.com/app/298630/The_Escapists/), decompile it with CTFAK, then build and run ResourceBuilder with the following arguments:
```
<path to orig_res> <decompiled dir> <game dir>
```

**ffmpeg must be present in PATH**

Full example:
```bash
cd ResourceBuilder
javac -d bin src/*.java
java -cp bin ResourceBuilder ../orig_res "~/CTFAK/The Escapists" "~/.local/share/Steam/steamapps/common/The Escapists/"
```

To compile the game you may use IntelliJ IDEA integrated with KEmulator nnmod, or Eclipse IDE (not newer than 2021-06!) with MTJ plugin and preprocessing enabled.

### Eclipse

[Eclipse setup guide for Windows](https://github.com/shinovon/KEmulator/wiki/Setting-up-Eclipse-IDE-for-J2ME-on-Windows-guide-(2025)) \
[Eclipse setup guide for Linux](https://github.com/shinovon/KEmulator/wiki/Setting-up-Eclipse-IDE-for-J2ME-on-Linux-x86_64-(2025)) \
[Preprocessing setup guide in Russian](https://github.com/shinovon/KEmulator/wiki/%D0%BF%D1%80%D0%B5%D0%BF%D1%80%D0%BE%D1%86%D0%B5%D1%81%D1%81%D0%B8%D0%BD%D0%B3-%D0%B2-%D1%8D%D0%BA%D0%BB%D0%B8%D0%BF%D1%81%D0%B5-%D1%81-%D0%BC%D1%82%D0%B6-(2025))

### IntelliJ IDEA

To open project in IntelliJ IDEA, open KEmulator nnmod, Tools>IntelliJ IDEA Support, go through setup steps, then in "Fix a project after clone" section, press "Choose a project".

Does not support preprocessing for platform-specific builds, so generic config will be used.\
see `src/Constants.java`

## Requirements

**API support**: MIDP 2.0, CLDC 1.1

**Keyboard**: Nokia/SE layout

**Screen size**: At least 176x220.\
Render is adaptive, but interface elements and textures won't scale, so higher than 640x360 is not recommended.

**Memory**: At least 1.5 MB of heap, and 1.5 MB of separate image memory.\
Tested heap usage is about 700 KB and 1 MB of image memory. (strongly depends on platform)

### Optional requirements

- M3G 1.1 support is required for lights and ambient effects.
- Alpha blending support is required for shadows.
- Audio mixing and WAV support are required to play sound effects.

## Tested devices

Main testing and target devices:
- Nokia E52 (240x320, S60v3.2, 600 MHz)
- Sony Ericsson K770 (240x320, A100, 220 MHz)
- Nokia 6700c (240x320, S40v6, 600 MHz)
- Nokia E72 (320x240, S60v3.2, 600 MHz)

Secondary (Works but low performance):
- Nokia E7 (640x360, S60v5.3, 600 MHz)
- Nokia E90 (240x320, S60v3.1, 332 MHz)
- Sony Ericsson W810 (176x220, A100, 110 MHz)
- Nokia N72 (176x208, S60v2.8, 220 MHz)
