# Karoo POI Map

A community-driven points of interest app for [Hammerhead Karoo](https://www.hammerhead.io/) bike computers. Add and share water points, hazards, bike shops, and more — visible directly on the Karoo ride map.

**[Download the latest APK](https://github.com/zenpeartree/karoo-poi-map/releases/latest/download/app-release.apk)**

## How It Works

The app adds a **map layer** to your Karoo. During rides, community-shared POIs appear on the map as you move. You can also add new points at your current location for other riders to see.

All POIs are synced automatically — add a water fountain on your ride, and every Karoo POI Map user will see it on theirs.

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

## Install

### Prerequisites

- Hammerhead Karoo (K2 or later) with developer mode enabled
- WiFi connection for initial POI sync (POIs are cached for offline rides)

### Option 1: Via Hammerhead Companion App (easiest)

1. Download the latest `app-release.apk` from [Releases](../../releases) on your phone
2. Tap **Share** on the downloaded file and select the **Hammerhead** companion app
3. The APK is automatically sent to your linked Karoo and installed

### Option 2: Via ADB

1. Install ADB on your computer ([install guide](https://developer.android.com/tools/adb))
2. Download the latest `app-release.apk` from [Releases](../../releases)
3. Connect to your Karoo via ADB:
   ```bash
   adb connect <karoo-ip>:5555
   ```
4. Install:
   ```bash
   adb install app-release.apk
   ```

## Usage

### Viewing POIs

Just start a ride — POIs appear on the map automatically as you move through areas where other riders have added points. The app caches nearby POIs so they remain visible even without a connection.

### Adding a POI

1. Open **Karoo POI Map** from the app drawer
2. Your current GPS location is shown automatically
3. Select a POI type from the grid
4. Optionally add a name (e.g., "Fountain near the park")
5. Tap **Add Point**

The POI is instantly available to all other users.

### Community Moderation

POIs can be upvoted or downvoted by the community. Points with too many downvotes are automatically hidden, keeping the map clean as conditions change (e.g., a water fountain that no longer works).

## License

Apache 2.0
