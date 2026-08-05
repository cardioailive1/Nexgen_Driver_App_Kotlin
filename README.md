# Nexgen Driver App — Android (Kotlin / Jetpack Compose)

A native port of the driver app: real Fused Location Provider GPS, a live
osmdroid (OpenStreetMap) map, real routing via the same free OSRM service the
web app uses, and a Socket.io client speaking the exact same real-time
protocol as the backend — no backend changes needed.

## Important — read this first

**I could not compile or run this app.** This sandbox has no Kotlin compiler,
no Android SDK, and no Gradle. Every file is written carefully and mirrors
the tested web app's logic (and the iOS Swift port) line-for-line where
possible, but the first real build will happen on your machine, not mine.
Expect to fix a handful of small issues on first build — that's normal, but
"written" and "verified" are not the same thing here.

There's also no Gradle wrapper jar in this zip (I can't download the Gradle
distribution binary from this sandbox). Opening the project in Android
Studio will prompt it to generate the wrapper automatically — just accept
that prompt on first open.

## Requirements

- Android Studio (Ladybug or newer recommended)
- An Android device or emulator running API 26+ (Android 8.0+)
- The backend already deployed (see the backend zip) — you need a real URL

## Setup

1. Open Android Studio → **Open** → select the `NexgenDriverAndroid` folder.
2. Let Android Studio sync Gradle and generate the wrapper when prompted.
3. Edit `app/src/main/java/com/corverxis/nexgendriver/Config.kt`:
   ```kotlin
   const val API_BASE = "https://<your-backend>.fly.dev"
   ```
4. Run on a device or emulator with Google Play Services (needed for the
   Fused Location Provider).

## Driver accounts

Drivers sign up with name/email/password and sign back in from any device —
the same account works whether they reinstall the app or switch phones. The
session token is stored in `SharedPreferences` (a production build should
move this to `EncryptedSharedPreferences` — plain SharedPreferences isn't
encrypted, and this is a deliberate scope tradeoff, not an oversight) and
validated against `/api/auth/me` on launch, so an expired or revoked token
cleanly returns to login instead of every request failing silently. "Log
out" and "change password" both live in the Account tab.

Two live pushes keep the app in sync without polling: an admin's
approve/reject decision arrives instantly over the existing socket
connection (`application:decision`), and if an admin deactivates the
account, an `account:deactivated` event forces an immediate logout instead
of waiting for the session token to expire naturally.

## Driver application & background check

New drivers hit an application screen before the dashboard — same flow as
the web app and iOS: personal info, license, insurance, vehicle details, and
8 document uploads via Android's built-in photo picker (`GetContent`, no
runtime permission needed on modern Android), uploaded directly to S3 via
presigned URLs. Submitting creates a Checkr background check; the driver
finishes that on Checkr's own hosted page (opened via an `Intent`), not in
this app — SSN never touches this app or its backend. The dashboard stays
locked until an admin approves the application (enforced server-side, not
just in this UI).

## Insights, ratings, and cancellations

A new Insights tab shows the driver's star rating (from riders, 1-5),
acceptance rate, cancellation rate, and recent written feedback from riders
— all pulled from `/api/driver/:id/insights` and refreshed live whenever a
new rating comes in over the socket (`trip:rated`), no polling needed.

Drivers can also cancel a trip after accepting but before the rider is
actually in the car (`Cancel trip` during the to-pickup phase) — this counts
against their cancellation rate, same as declining a request before ever
accepting counts against acceptance rate. Both counters are tracked
server-side, not computed from a full history scan.

## Tips

Insights now also shows total tips and a recent-tips list. Tips are a
**separate charge from the fare** — the rider adds one from the web rider
app after the trip, charged 100% to the driver (no platform cut, unlike the
60/20/20 fare split). This app only displays the result; tip collection UI
lives in the rider web app, since riders don't use these native apps.

## Road-distance matching & rewards

Incoming requests now show whether they were matched using real road
distance/time (`matched`) or sent directly because only one driver was
available (`direct`) — the same badge shown on the web app's request card.

Insights also shows a reward tier — Silver (200 pts) / Gold (400) / Premium
(600) / Diamond (1000) — with a progress bar to the next tier and a
celebratory alert the moment a trip pushes the driver into a new one, pushed
live over the socket (`reward:tierUp`). Points are earned +10 per completed
trip, +5 for a 5-star rating, +2 per tip — a design choice made when this was
built, not a specification; change it in `REWARD_TIERS`/`awardPoints()` on
the backend if you want a different formula.

## Reward point schedule

Trip-completion points now follow a time-of-day schedule instead of a flat
amount: 1 point normally, 3 points during bonus hours — every night
9:00 PM–2:00 AM, plus weekends 11:00 AM–3:00 PM, all evaluated in US
Eastern time (`America/New_York`, which auto-adjusts EST/EDT). If the
runtime's timezone data is somehow unavailable, the backend falls back to
its own local clock rather than crashing. The trip-complete screen shows how
many points were earned and whether the bonus applied.

## Fare calculation — mileage + real gas prices

The per-km rate now has a real fuel component: `(current gas price ÷ 25
assumed MPG) ÷ 1.60934`, added to a flat non-fuel per-km rate. Gas price
comes from the backend's `/api/fare/rates` endpoint (EIA's weekly US average,
cached 24h, with a manual fallback if unset).

**A real bug this fixes**: the incoming-request fare estimate on this app
used to be computed with a completely different, made-up formula
(`$2.50 + miles × $1.50`) from what actually got charged at trip
completion. It now calls `/api/fare/rates` and uses the exact same numbers
that will be billed, so the estimate can't silently disagree with the final
fare the way it used to. The trip-complete screen also shows the gas price
actually used for that trip.

## What's real vs. not implemented here

- **Real**: Fused Location Provider GPS, osmdroid live map, OSRM real road
  routing (same public service as the web app), Socket.io real-time events
  identical to the web/iOS protocol (`driver:online`, `request:new`,
  `trip:assigned`, `trip:complete`, etc.), fare/payout math matching the
  60/20/20 split.
- **Payments**: the driver side only needs Stripe's *hosted onboarding link*,
  opened via an `Intent.ACTION_VIEW` to the browser — no Stripe SDK needed on
  this app. Card collection lives in the rider app, not here.
- **Not implemented**: push notifications for new requests while the app is
  backgrounded/killed (would need FCM + a server-side trigger), background
  location tracking while the app isn't foregrounded (the manifest requests
  the permission, but there's no foreground service wired up to keep updates
  flowing when the screen is off), and Play Store submission assets
  (adaptive icon variants, screenshots, privacy disclosures).
- **Launcher icon** is a plain PNG dropped into `mipmap-hdpi`/`mipmap-xxhdpi`,
  not a proper adaptive icon — good enough to run, not production-ready.
  Regenerate it with Android Studio's Image Asset tool before shipping.

## Project structure

```
NexgenDriverAndroid/
├── settings.gradle.kts
├── build.gradle.kts
└── app/
    ├── build.gradle.kts              # all dependencies (Compose, osmdroid, Retrofit, Socket.io, Play Services Location)
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                        # launcher icon, strings, theme
        └── java/com/corverxis/nexgendriver/
            ├── MainActivity.kt           # permission flow + screen routing
            ├── Config.kt                  # ← set your backend URL here
            ├── data/Models.kt              # data classes matching the backend
            ├── network/
            │   ├── ApiClient.kt              # Retrofit REST client
            │   ├── SocketManager.kt           # Socket.io — same events as web/iOS
            │   └── RoutingService.kt           # OSRM routing
            ├── location/LocationProvider.kt   # Fused Location wrapper
            ├── viewmodel/
            │   ├── DriverViewModel.kt           # State machine — mirrors frontend/app.js
            │   └── DriverViewModelFactory.kt
            └── ui/
                ├── theme/                        # Corverxis colors
                └── screens/                        # Login, Dashboard, Request overlay, Trip, Complete, Account, Map
```

## Pairs with

The backend (Express + Socket.io + Prisma/Postgres + Stripe) is the same one
deployed for the web app and iOS app — see `nexgen-driver-app-backend.zip`.
All three clients (web, iOS, Android) speak to it identically.
