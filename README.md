# OclAx

[Español](README.es.md) · **English**

**OclAx = Open Content, Local Access & eXchange**

OclAx is a local-first Android content tray for quickly receiving, organizing, reusing and selecting content across apps.

**Your content, ready where you need it.**

## What OclAx does

Share text, images or files to OclAx and it keeps a private local copy that you can quickly find, open, copy or share again.

```text
Share text / image / file
        ↓
       OclAx
        ↓
private temporary copy + categories + search
        ↓
copy / open / share / choose from another app
```

OclAx also includes **My device**, where you can browse real device files and installed applications after granting the required Android access.

## Main features

- Android Share Sheet integration;
- private local temporary storage;
- configurable auto-cleanup and Pinned items;
- search and visual categories;
- multi-selection for sharing, pinning/unpinning and deleting OclAx copies;
- multi-selection in My device for sharing files or installed applications;
- automatic clipboard copy for shared text and images;
- image, video and PDF thumbnails;
- rename support;
- read-only DocumentsProvider integration;
- custom **Choose with OclAx** picker for other apps;
- safe export of installed APK packages without private app data;
- smart recent-screenshot cleanup: when a verifiable screenshot is shared within 60 seconds of creation, OclAx can also delete the original when the OclAx copy expires;
- Spanish and English interface based on the Android system language.

## Privacy and network

OclAx is designed to work locally.

- No `INTERNET` permission.
- No telemetry.
- No LAN transfer, pairing or Syncthing runtime.
- Shared content is stored locally on the device.
- Installed-app export uses only installation APK files, never private app data.

## Permissions

Some **My device** features require broad Android access:

- `MANAGE_EXTERNAL_STORAGE`: browse and manage real device files;
- `QUERY_ALL_PACKAGES`: list installed applications;
- `REQUEST_INSTALL_PACKAGES`: explicit APK installation flow;
- `RECEIVE_BOOT_COMPLETED`: reschedule local retention cleanup after reboot.

The private OclAx tray continues to work even when broad file access is not granted.

## Languages

OclAx currently supports:

- English;
- Spanish.

The interface follows the Android system/app locale.

## Installation

Public distribution is planned through **GitHub Releases**.

When a signed production release is available:

1. open the latest GitHub Release;
2. download the OclAx APK;
3. allow installation from your browser/file manager when Android asks;
4. install or update OclAx.

Only install APKs published from this official repository.

## Development

Requirements:

- JDK 17;
- Android SDK 36;
- Gradle 9.6.0.

```bash
gradle :app:testDebugUnitTest
gradle :app:lintDebug
gradle :app:assembleDebug
gradle :app:assembleRelease
```

Before modifying the project, read `AGENTS.md`.

## Release status

The local Android release candidate has passed the current technical and physical QA gates. Production signing and the first public GitHub Release remain separate release steps.
