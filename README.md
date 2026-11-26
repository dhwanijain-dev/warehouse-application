# Warehouse Scan

Android application built for fabric-roll batch scanning on Zebra TC-series handhelds with SE47 hardware imagers. Works fully offline by default.

## Features
- **Local Login Database**: On-device Room user store seeded from `users.json`, with full User Management (add, edit, deactivate, delete, JSON import/export).
- **Offline Email Modes**: Intent mail app attachment (`intent`), Save to Downloads (`saveonly`), and SMTP delivery (`smtp`).
- **Location Setting**: Rack, Zone, Setting input, config-driven duplicate scan policies (`reject`, `warn`, `allow`), and TXT file generation.
- **Dispatch Verification**: Upload dispatch TXT file, live matching dashboard (Matched, Unmatched, Pending), and invariant enforcement.
- **Demo Build Flavor**: Build variant with visible "DEMO" banner, local login, save-only email, and fake scanner.

## Build & Test
- Build debug APK: `./gradlew assembleDebug`
- Build demo release APK: `./gradlew assembleDemoRelease`
- Run tests: `./gradlew testProdDebugUnitTest`
- Code quality check: `./gradlew detekt`
