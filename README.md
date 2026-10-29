# TheEscapists-J2ME

The Escapists fan demake for J2ME platform.

![screenshot](/screenshot1.png) ![screenshot](/screenshot2.png)

## Project state

- NPC behavior is finished.
- Original maps parser is finished.
- Rendering engine is finished, but needs optimization.
- The game can be beaten without cheats. (except some maps)
- Some important mechanics are not yet implemented, e.g. journal and favors.

See [TODO.md](/TODO.md) for more details

## Disclaimer

All rights to the original game belong to Mouldy Toof Studios and Team17.\
This repository does not contain any original game assets or decompiled code.

## Building

You have to obtain legal copy of The Escapists [(Steam)](https://store.steampowered.com/app/298630/The_Escapists/), decompile it with [Nebula](https://github.com/AITYunivers/NebulaFD), then build and run ResourceBuilder with the following arguments:
```
<path to orig_res> <decompiled dir> <game dir>
```

**ffmpeg must be present in PATH**

Full example:
```bash
cd ResourceBuilder
javac -d bin src/*.java
java -cp bin ResourceBuilder ../orig_res "~/Nebula/Dumps/The Escapists" "~/.local/share/Steam/steamapps/common/The Escapists/"
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

## Device requirements

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

See [Releases.md](/Releases.md)

## Tested devices

Main testing and target devices:

| Device             | Build | Screen                      | Platform               |
|--------------------|-------|-----------------------------|------------------------|
| Nokia E52          | S60   | 240x320                     | S60v3.2, ARM11 600 MHz |
| Nokia E72          | S60   | 320x240                     | S60v3.2, ARM11 600 MHz |
| Nokia 6700c        | S40   | 240x320                     | S40v6, ARM11 600 Mhz   |
| Sony Ericsson C510 | S40   | 240x320                     | A200                   |
| Sony Ericsson K770 | S40   | 240x320                     | A100, ARM9 220 MHz     |
| Nokia E6           | E6/E7 | 320x240 upscaled to 640x480 | S60v5.2, ARM11 680 MHz |


Secondary (Works but low performance):

| Device             | Build                         | Screen                      | Platform               |
|--------------------|-------------------------------|-----------------------------|------------------------|
| Nokia E90, N95     | S60, SFX off                  | 240x320                     | S60v3.1, ARM11 332 MHz |
| Nokia E7           | E6/E7                         | 320x240 upscaled to 640x360 | S60v5.3, ARM11 680 MHz |
| Sony Ericsson W810 | S40, SFX off                  | 176x220                     | A100, ARM9 110 MHz     |
| Nokia N72          | S40, everything off / Minimum | 176x208                     | S60v2.8, ARM9 220 MHz  |
| PS Vita Adrenaline | Minimum                       | 480x272                     | PSPKVM, MIPS 300 MHz   |
| Sony Ericsson P1i  | S40, everything off / Minimum | 240x320                     | UIQ 3.0, ARM9 208 MHz  |
