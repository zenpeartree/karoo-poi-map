# Karoo POI Map

Karoo POI Map adds a shared POI layer to [Hammerhead Karoo](https://www.hammerhead.io/) bike computers. It shows nearby community-submitted points on the ride map and lets you add new ones from the device.

**[Download the latest APK](https://github.com/zenpeartree/karoo-poi-map/releases/latest/download/app-release.apk)**

## Current State

- Shows nearby POIs as a Karoo map layer during rides
- Fetches POIs from Firebase and caches them locally for offline use
- Lets you add POIs from the app drawer or from Control Center while riding
- Lets you review nearby cached POIs and upvote or downvote them from the app drawer or Control Center
- Refreshes the visible map layer immediately after a successful add

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

### Viewing POIs

Start a ride and enable the POI map layer on the Karoo map. Nearby POIs are fetched as you move and cached locally so recently seen points remain available when connectivity drops.

### Adding a POI During a Ride

1. Open Control Center
2. Tap the **POI Map** action
3. Wait for the current location to load
4. Choose a POI type
5. Optionally enter a name
6. Tap **Add Point**

After saving, the add screen closes and returns to the ride map.

### Adding a POI From the App Drawer

1. Open **Karoo POI Map**
2. Wait for the current location to load
3. Choose a POI type
4. Optionally enter a name
5. Tap **Add Point**

### Voting on Nearby POIs

From the app drawer:

1. Open **Karoo POI Map**
2. Tap **Review Nearby POIs**
3. Wait for the nearby cached list to load
4. Upvote useful POIs or downvote stale ones

During a ride:

1. Open Control Center
2. Tap the **POI Map** vote action
3. Review nearby cached POIs
4. Submit an upvote or downvote

Votes are submitted once per rider account and the map refreshes after a successful vote.

## Notes

- The app uses anonymous Firebase authentication in the background
- POIs are cached on-device in shared preferences

## License

Apache 2.0
