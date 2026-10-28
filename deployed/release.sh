#!/bin/bash

REVISION=`git describe --tags --always HEAD`
if [[ -z "$TE_DEPLOY_DIR" ]]; then
TE_DEPLOY_DIR=$(pwd)
fi

# Minimum
cd Minimum || exit 1
cp TE.jar TE_Minimum.jar
mkdir META-INF
unzip -p TE_Minimum.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
dos2unix META-INF/MANIFEST.MF && \
sed -i "s/TE-Build: development/TE-Build: Minimum ($REVISION)/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
zip -u TE_Minimum.jar META-INF/*
cp TE.jar "$TE_DEPLOY_DIR/TE_Minimum.jar"

# S40
cd ../S40 || exit 1
cp TE.jar TE_S40.jar
mkdir META-INF
unzip -p TE_S40.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
dos2unix META-INF/MANIFEST.MF && \
sed -i "s/TE-Build: development/TE-Build: S40 ($REVISION)/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
zip -u TE_S40.jar META-INF/*
cp TE_S40.jar "$TE_DEPLOY_DIR/TE_S40.jar"

# S60
cd ../S60 || exit 1
cp TE.jar TE_S60.jar
cp TE.jar TE_S60_scaled.jar
cp TE.jar TE_E6_E7.jar
mkdir META-INF
unzip -p TE_S60.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
dos2unix META-INF/MANIFEST.MF && \
sed -i "s/TE-Build: development/TE-Build: S60 ($REVISION)/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
zip -u TE_S60.jar META-INF/*
cp TE_S60.jar "$TE_DEPLOY_DIR/TE_S60.jar"

unzip -p TE_S60_scaled.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
dos2unix META-INF/MANIFEST.MF && \
sed -i "s/TE-Build: development/TE-Build: S60 ($REVISION)\nNokia-MIDlet-Original-Display-Size: 320, 240/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
zip -u TE_S60_scaled.jar META-INF/*
cp TE_S60_scaled.jar "$TE_DEPLOY_DIR/TE_S60_scaled.jar"

unzip -p TE_E6_E7.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
dos2unix META-INF/MANIFEST.MF && \
sed -i "s/TE-Build: development/TE-Build: S60 ($REVISION)\nNokia-MIDlet-Original-Display-Size: 320, 240/g" META-INF/MANIFEST.MF && \
sed -i "s/Nokia-MIDlet-On-Screen-Keypad: gameactions/Nokia-MIDlet-On-Screen-Keypad: no/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
zip -u TE_E6_E7.jar META-INF/*
cp TE_E6_E7.jar "$TE_DEPLOY_DIR/TE_E6_E7.jar"




