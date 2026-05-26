#!/bin/bash

REVISION=`git describe --tags --always HEAD`

# Minimum
cd Minimum || exit 1
cp TE.jar ../TE_Minimum.jar
cd ..
mkdir META-INF
unzip -p TE_Minimum.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
dos2unix META-INF/MANIFEST.MF && \
sed -i "s/TE-Build: development/TE-Build: Minimum ($REVISION)/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
zip -u TE_Minimum.jar META-INF/*

# S40
cd S40 || exit 1
cp TE.jar ../TE_S40.jar
cd ..
mkdir META-INF
unzip -p TE_S40.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
dos2unix META-INF/MANIFEST.MF && \
sed -i "s/TE-Build: development/TE-Build: S40 ($REVISION)/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
zip -u TE_S40.jar META-INF/*

# S60
cd S60 || exit 1
cp TE.jar ../TE_S60.jar
cp TE.jar ../TE_S60_scaled.jar
cd ..
mkdir META-INF
unzip -p TE_S60.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
dos2unix META-INF/MANIFEST.MF && \
sed -i "s/TE-Build: development/TE-Build: S60 ($REVISION)/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
zip -u TE_S60.jar META-INF/*

unzip -p TE_S60_scaled.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
dos2unix META-INF/MANIFEST.MF && \
sed -i "s/TE-Build: development/TE-Build: S60 ($REVISION)\nNokia-MIDlet-Original-Display-Size: 320, 240/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
zip -u TE_S60_scaled.jar META-INF/*




