# Release files

- **ResourceBuilder.jar** - **Java SE** tool for building resources from decompiled assets, Java 8+ is required.
- **TE_S40.jar** - Optimized for S40v5+ S60v2
- **TE_S60.jar** - Optimized for S60v3
- **TE_S60_scaled.jar** - Upscaled 320x240 image with virtual keyboard for S60v5, S^3
- **TE_E6_E7.jar** - Upscaled 320x240 image without virtual keyboard for Nokia E6, E7
- **TE_SE.jar** - Optimized for Sony Ericsson, uses Mascot Capsule for light effects
- **TE_Minimum.jar** - Reduced build for devices with low memory or no M3G support

# Building resources

First, dump assets of the game with [Nebula](https://github.com/AITYunivers/NebulaFD), then run ResourceBuilder.jar.\
After it finishes, add generated resources to downloaded TE_*.jar file.

### How to run ResourceBuilder.jar

Linux example (you'll need Windows version of the game):
```bash
mkdir TE_res && java -jar ResourceBuilder.jar TE_res "~/Nebula/Dumps/The Escapists" "~/.local/share/Steam/steamapps/common/The Escapists/"
cd TE_res && zip -u "../TE_S40.jar" "*"
```

Windows example:
```powershell
mkdir TE_res
java -jar ResourceBuilder.jar TE_res "Nebula\Dumps\The Escapists" "C:\Program Files (x86)\Steam\steamapps\common\The Escapists"
Rename-Item -Path "TE_S40.jar" -NewName "TE_S40.zip"
Compress-Archive -Path "TE_res\*" -Update -DestinationPath TE_S40.zip
Rename-Item -Path "TE_S40.zip" -NewName "TE_S40.jar"
```