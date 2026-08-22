# Volume Key Track Control Module

![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/Hepolise/VolumeKeyMusicManagerModule/build.yml)
![GPLv3 License](https://img.shields.io/badge/License-GPL%20v3-yellow.svg)

An Xposed module that allows you to control media playback using the volume buttons.

Inspired by [GravityBox](https://github.com/GravityBox/GravityBox).

## Features

* Skip to the next or previous track
* Play or pause media
* Seek forward or backward
* Works when the screen is off
* Configurable long press duration
* Configurable seek duration
* Swap volume button actions
* Optional vibration feedback
* Application whitelist and blacklist

## Usage

By default:

* **Long press Volume Up** — Next track
* **Long press Volume Down** — Previous track
* **Long press both buttons** — Play / Pause

Additional actions can be configured in the module settings.

## Requirements

* Android 11+
* Xposed-compatible framework

## Tested on

* OnePlus 13 (Android 16)

## Issues

[Open an issue](https://github.com/Hepolise/VolumeKeyTrackControlModule/issues/new) if you encounter a problem or have a suggestion.
