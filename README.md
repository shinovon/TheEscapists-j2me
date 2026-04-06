# TheEscapists-J2ME

The Escapists fan demake for J2ME platform.

![screenshot](/screenshot.png)

## Disclaimer

All rights to the original game belong to Mouldy Toof Studios and Team17.
<br>
This repository does not contain any original game assets or decompiled code.

## Building

You have to obtain legal copy of The Escapists [(Steam)](https://store.steampowered.com/app/298630/The_Escapists/), decompile it with CTFAK, then build and run ResourceBuilder with the following arguments:
```
<output res dir> <decompiled dir> <game dir> <map name>
```

Full example:
```bash
cd ResourceBuilder
javac -d bin src/*.java
java -cp bin ResourceBuilder ../res "~/CTFAK/The Escapists" "~/.local/share/Steam/steamapps/common/The Escapists/" shanktonstapen
```

For compiling the game you may use IntelliJ IDEA integrated with KEmulator nnmod, or Eclipse IDE (not newer than 2024-06!) with MTJ plugin.

To open project in IntelliJ IDEA, open KEmulator nnmod, Tools>IntelliJ IDEA Support, go through setup steps, then in "Fix a project after clone" section, press "Choose a project".

## Requirements

MIDP 2.0, CLDC 1.1

Screen size: At least 176x220.
<br>
Render is adaptive, but interface elements and textures won't scale, so higher than 640x360 is not recommended.

Memory: At least 1.5 MB of heap, and 1.5 MB of separate image memory.

Tested heap usage is about 700 KB and 1 MB of image memory. (strongly depends on platform)

### Optional requirements

- M3G 1.1 support is required for lights and ambient effects.
- Alpha blending support is required for shadows.
- Audio mixing and WAV support are required to play sound effects.

see src/Constants.java
