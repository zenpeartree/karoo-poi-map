# Karoo POI Map

A community-driven points of interest app for [Hammerhead Karoo](https://www.hammerhead.io/) bike computers. Add and share water points, hazards, bike shops, and more — visible directly on the Karoo ride map.

**[Download the latest APK](https://github.com/zenpeartree/karoo-poi-map/releases/latest/download/app-release.apk)**

## How It Works

The app adds a **map layer** to your Karoo. During rides, community-shared POIs appear on the map as you move. You can also add new points at your current location for other riders to see.

All POIs are synced via Firebase — add a water fountain on your ride, and every Karoo POI Map user will see it on theirs.

## POI Types

| Type | Icon | Description |
|------|------|-------------|
| Water | 💧 | Fountains, taps, water refill spots |
| Dogs | ⚠️ | Aggressive dogs or animal hazards |
| Road Hazard | ⚠️ | Potholes, broken roads, gravel, construction |
| Bike Shop | 🔧 | Bike shops and repair services |
| Cafe | ☕ | Cafes and coffee stops |
| Restroom | 🚻 | Public restrooms |
| First Aid | ⛑️ | First aid and medical facilities |

## Getting Started

### Prerequisites

- Hammerhead Karoo (K2 or later) with developer mode enabled
- ADB installed on your computer
- WiFi connection for initial POI sync (POIs are cached for offline rides)

### Install

1. Download the latest `app-release.apk` from [Releases](../../releases)
2. Connect to your Karoo via ADB:
   ```bash
   adb connect <karoo-ip>:5555
   ```
3. Install:
   ```bash
   adb install app-release.apk
   ```

### Usage

**Viewing POIs:**
- Just start a ride — POIs appear on the map automatically
- The app fetches nearby POIs as you move and caches them locally

**Adding a POI:**
1. Open **Karoo POI Map** from the app drawer
2. Your current GPS location is shown
3. Select a POI type from the grid
4. Optionally add a name (e.g., "Fountain near the park")
5. Tap **Add Point**

**Community moderation:**
- POIs with too many downvotes are automatically hidden
- This keeps the map clean as conditions change

## Build from Source

### Prerequisites

- JDK 17+
- Android SDK (API 34)
- A GitHub personal access token with `read:packages` scope
- A Firebase project with Firestore and Anonymous Auth enabled

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/zenpeartree/karoo-poi-map.git
   cd karoo-poi-map
   ```

2. Add GitHub Packages credentials to `~/.gradle/gradle.properties`:
   ```properties
   gpr.user=YOUR_GITHUB_USERNAME
   gpr.key=YOUR_GITHUB_TOKEN
   ```

3. Set up Firebase:
   - Create a project at [console.firebase.google.com](https://console.firebase.google.com)
   - Add an Android app with package `com.zenpeartree.karoopoimap`
   - Download `google-services.json` to `app/`
   - Enable Firestore Database and Anonymous Authentication
   - Deploy the security rules from `firestore.rules`

4. Build:
   ```bash
   ./gradlew assembleDebug
   ```

5. Run tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

## Architecture

```
Karoo Map
    ▲
    │ ShowSymbols(List<Symbol.POI>)
    │
KarooPoiExtension (mapLayer=true)
    │ startMap() → emits symbols as rider moves
    │
PoiRepository
    ├─ Local JSON cache (SharedPreferences)
    └─ Firebase Firestore
         └─ GeoHash spatial queries (~5km cells)
```

- **karoo-ext 1.1.8** — Karoo Extension SDK for map layer integration
- **Firebase Firestore** — Community POI database with GeoHash indexing
- **Firebase Anonymous Auth** — Device-level identity for voting and attribution

## License

Apache 2.0
