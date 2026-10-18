# Brief upgrade report

Zakk Travel 2 is a working offline Android portfolio app, not a live hotel-booking service. It keeps the original Java/Android foundation and bundled photos while replacing placeholder behavior with persistent local features.

| Area | Original coursework | Version 2 |
| --- | --- | --- |
| Accounts | Fixed admin login; registration interface | Local account creation, hashed passwords, persistent sessions, sign-out and account deletion |
| Data/backend | No persistent account/trip database | Private SQLite users, favorites and plans; foreign keys and account-scoped queries |
| Discovery | City/hotel lists and navigation | Shared typed catalog, live name/country search, category filters and adaptive RecyclerView cards |
| Saved/trips | Placeholder destinations | Persistent saved places and dated city/hotel plans; validated guests/nights, demo totals and cancellation |
| UI | Original multi-screen prototype | Consistent forest-green theme, rounded cards, readable hierarchy, photo galleries and empty/error states |
| Correctness | City rating constructor issue | Explicit sample ratings, integer-cent totals, date checks and automated checks |

The frontend is native Java Activities plus RecyclerView and XML themes. `MainActivity` handles accounts, `HomeActivity` handles Explore/Saved/Trips/Account, and `DetailActivity` handles galleries and planning. Navigation uses explicit Intents.

The backend is local: `TravelStore` performs parameterized SQLite operations and derives record ownership from the current session. Password protection uses salted PBKDF2-HMAC-SHA256 with 600,000 iterations; hashing runs off the UI thread. Account deletion cascades to that account's saved places and plans.

There is no server, remote authentication, email verification/recovery, live availability, cloud sync, payment processing or real reservation. Prices and ratings are examples; hotel plans never contact a provider. The local database is not encrypted against device compromise.

See `VERIFICATION.md` for measured checks and limitations. `screenshots/overview.png` and the individual PNGs are actual emulator captures, not generated mockups. The original 2024 archive remains separate; these improvements are a later upgrade.
