# Verification

## Completed checks

- `python3 scripts/check_core.py`: **86 checks passed** against the real domain classes. Covers input boundaries, email normalization, password hashing and verification, malformed hashes, date rules, checked money totals, catalog filtering and gallery mappings.
- `scripts/assemble_sdk.py`: app and instrumentation APK resource linking, Java compilation, D8 dexing, alignment and signature verification passed using Android SDK 34/build-tools 34.0.0.
- APK metadata: `com.zakk.travel`, version 2.0 (code 2), minimum API 26, target API 34, expected launcher Activity. The app declares no Internet permission.

## Device checks

**Final combined run: 7 scenarios passed, 0 failures** on an Android 9 / API 28 x86_64 software emulator. Raw final output is in `DEVICE_TESTS.txt` and ends with `Tests run: 7, failures: 0` and `INSTRUMENTATION_CODE: -1`.

The scenarios cover real registration/login/duplicate and wrong-password rejection; account isolation; persistence and cascading deletion; plan validation/cancellation; quoted input and session guards; deleting only the current account; and a UI journey through search, category filters, saving a place, detail navigation, planning, Trips and removal.

The authentication scenario uses real password hashing and verification. Other scenarios use test-only fixture accounts/session setup to avoid repeating expensive KDF work on a software emulator. They still exercise the actual Android SQLite store and Activity/UI implementations. No fixture or test entrypoint is included in the app APK.

An initial long-running suite timed out. A subsequent UI test exposed an uppercase dialog-button lookup mismatch; the lookup was made case-insensitive and bounded, then the complete suite was rerun successfully. The earlier partial runs are not the basis for the final passing count.

Actual screenshots are in `screenshots/`. The overview only arranges/resizes real captures; it does not invent UI. Test screenshots use synthetic account/plan data.

Delivered APK SHA256: `9cc4e3fd1ed3bbf84fac4f0afb0b072c8372a388009edfecc77b5b103ecadd03`. Its v2 and v3 APK signatures verify. It is the app build installed for the device checks; later source edits affect tests and documentation, not the app implementation.

## Reproduction

The normal development entrypoint is Android Studio/Gradle. A second, Linux-tested SDK assembly path is supplied for environments where the Android Gradle Plugin cannot be downloaded:

```sh
python3 scripts/check_core.py
python3 scripts/assemble_sdk.py --sdk /absolute/path/to/android-sdk --out /absolute/path/to/build
adb install -r /absolute/path/to/build/ZakkTravel_v2_Debug.apk
adb install -r /absolute/path/to/build/ZakkTravel_v2_Tests.apk
adb shell am instrument -w -r -e captureUi true com.zakk.travel.test/com.example.login.SmokeTests
```

Inspect the instrumentation summary and failure count, not just the adb exit status. Device tests use prefixed test-only databases and preferences and refuse cleanup without the isolated Application context.

For a faster non-authentication-only rerun, add `-e skipAuthentication true`; that executes six scenarios and does not count authentication as passed. Optional screenshots are written to the app cache with the `instrumented-review-` prefix and can be retrieved with `adb exec-out run-as com.zakk.travel cat cache/instrumented-review-home.png` (redirect stdout to a PNG file).

The review APK is debug-signed, not a store release. The signing key is not distributed. A later locally built APK may have a different debug key and require uninstalling this review build before installation; uninstalling removes its local data.

## Limits

- Android Studio sync, the Gradle build, supplied GitHub Actions workflow, Windows, physical devices and other Android versions have not been verified in this recovered run.
- No remote service, live availability, real reservations, payments, email verification, recovery or cloud sync is implemented or tested.
- This is a functional portfolio upgrade, not a production security audit. Local data is not encrypted against device compromise.
