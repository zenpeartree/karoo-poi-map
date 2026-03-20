# Karoo POI Map

Karoo POI Map adds a shared POI layer to [Hammerhead Karoo](https://www.hammerhead.io/) bike computers. It gives you dedicated ride fields and tile entrypoints so you can add a point or review nearby ones without depending on the notification tab.

**[Download the latest APK](https://github.com/zenpeartree/karoo-poi-map/releases/download/v1.0.3-beta/app-release.apk)**

## What It Does

- Shows nearby community-submitted POIs as a Karoo map layer during rides
- Adds an **Add POI** ride field that opens the add screen directly
- Adds a **Review POIs** ride field that opens the nearby vote screen directly
- Adds an **Add POI** tile that opens the add screen directly
- Adds a **Review POIs** tile that opens the nearby vote screen directly
- Keeps ride notifications available as a fallback entrypoint during rides
- Fetches POIs from Firebase and caches them locally for offline use
- Refreshes the visible map layer immediately after a successful add or vote

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

### Requirements

- Hammerhead Karoo with developer mode enabled
- Wi-Fi or mobile connectivity for initial POI sync and uploads

### Option 1: Hammerhead Companion App

1. Download the latest `app-release.apk` from [Releases](../../releases) on your phone
2. Tap **Share** on the APK and choose the **Hammerhead** companion app
3. The companion app sends it to your linked Karoo for installation

### Option 2: ADB

1. Install ADB on your computer ([install guide](https://developer.android.com/tools/adb))
2. Download the latest `app-release.apk` from [Releases](../../releases)
3. Connect to the Karoo:
   ```bash
   adb connect <karoo-ip>:5555
   ```
4. Install the APK:
   ```bash
   adb install app-release.apk
   ```

## Usage

### Ride Setup

1. Start a ride
2. Enable the **Karoo POI Map** layer on the Karoo map
3. Add the **Add POI** and **Review POIs** ride fields to a ride page if you want one-tap in-ride access
4. Use either the ride fields or the tiles to log a new point or review nearby cached points

Nearby POIs are fetched as you move and cached locally so recently seen points remain available when connectivity drops.

### Ride Fields

1. Open the Karoo ride page editor
2. Add the **Add POI** field to a page
3. Add the **Review POIs** field to a page
4. Tap either field during a ride to open the matching screen

These ride fields are the most direct way to launch the POI flows from the ride screen.

### Add POI Tile

1. Open the **Add POI** tile
2. Wait for the current location to load
3. Choose a POI type
4. Optionally enter a name
5. Tap **Add Point**

After saving, the add screen closes and returns to the ride flow.

### Review POIs Tile

1. Open the **Review POIs** tile
2. Wait for the nearby cached list to load
3. Upvote useful POIs or downvote stale ones
4. Submit your vote

Votes are submitted once per rider account and the map refreshes after a successful vote.

### Fallback Entry Points

If you prefer, or if you are troubleshooting the tile behavior:

1. Open **Karoo POI Map** from the app drawer
2. Add a POI from the main screen
3. Tap **Review Nearby POIs** to open the vote screen
4. During a ride, the Control Center notifications still act as backup shortcuts

## Notes

- The app uses anonymous Firebase authentication in the background
- POIs are cached on-device in shared preferences
- This is currently a beta release

## License

Apache 2.0
