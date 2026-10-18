# Architecture and scope

The frontend uses native Android Java Activities. `MainActivity` handles sign-in and registration; `HomeActivity` provides Explore, Saved, Trips and Account tabs; `DetailActivity` shows city and hotel information, galleries and the planning dialog. `Ui` shares typography, colors, spacing and bitmap downsampling. RecyclerView cards adapt to one, two or three columns according to screen width and font scale. Search and tab state survive Activity recreation.

The local backend is `TravelStore`, an Android SQLiteOpenHelper with parameterized SQL and foreign keys. Users have normalized unique email identifiers and salted password hashes. Favorites use a composite user/place key. Plans are unique by user, place and arrival date. Every read and cancellation derives its owner from the current session; callers cannot supply arbitrary user IDs. Deleting an account cascades only its own records. Favorite toggles are transactional.

`PasswordHasher` uses PBKDF2-HMAC-SHA256 with 600,000 iterations, a random 128-bit salt and a 256-bit derived key. Hash verification uses constant-time comparison. Expensive authentication work runs off the UI thread. Password fields are excluded from saved view state. The private session stores only an account ID, and every session read verifies that it still exists. Backups are disabled, internal screens are not exported, and the production manifest requests no Internet permission.

`Rules` validates account inputs, dates, nights and guests. Money is represented as integer cents with checked multiplication. City visits last one day; hotel plans allow 1–30 nights and 1–6 guests. The sample room price is multiplied by nights, not guests. Taxes and fees are not calculated. Catalog ratings are illustrative and explicitly initialized, fixing the original zero-rating bug.

This is an offline portfolio app. It has no remote backend, inventory provider, real reservation or payment connection. Demo plans never contact a hotel. Email-shaped identifiers are not verified email ownership. There is no password reset or cloud sync. The database is private to the app sandbox but is not encrypted against a compromised device.

Device tests replace the application context with a test-only application that prefixes database and preference names. They fail before cleanup if that isolated context is absent. The test APK is separate from the app APK, and no testing entrypoint is included in the production manifest.

## Source provenance

Original file: `219J101.zip`, 13,100,095 bytes, submitted in July 2024.

SHA256: `8bc860a3982bf86d97bd35f1ad09c173e1e6bae9904dd421dca1746e662ea3b2`

The original archive is preserved separately. Version 2 is a later upgrade, not a claim that the added backend or redesigned UI existed in the 2024 coursework. Source namespace is retained for continuity; the new application ID allows separate installation.
