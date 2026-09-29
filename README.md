# ResQ Android 0.8.1 — Live Rescue Intelligence

## What is new in 0.8.1

- Large blue person/device marker labelled `YOU • This device`
- Device marker is added after other markers so it remains visually prominent
- Map automatically centres on the first available GPS fix at street-level zoom
- My Location button remains available to recenter after panning
- Rescue legend now identifies the device marker

Version 0.8.0 extends the working ResQ application with battery-aware live location, nearest offline rescue-service search, distance calculation, an honest offline directional-path fallback, and on-device emergency-photo understanding. Bluetooth, Wi-Fi Direct, Room, the existing classifier, Rescue Mode, and the PMTiles map architecture are preserved.

## What is new in 0.8.0

- Live Fused Location updates while the Map screen is open (15-second interval and 10-metre movement threshold)
- Fresh high-accuracy location request before an emergency packet is saved
- Last-known-location fallback with a visible stale-location indication
- Find Nearby categories for hospitals/clinics, fire, police, pharmacies, fuel/water, and shelters
- Haversine distance calculation from the current GPS position
- Facility selection, exact coordinates, and an offline straight-line directional path
- Explicit warning that the path is not turn-by-turn road navigation
- Camera and gallery emergency-photo input
- System camera contract, so ResQ does not request unnecessary direct camera access
- Bundled ML Kit image labeling that remains available offline
- Conservative generated descriptions that distinguish visible evidence from user-provided context
- Mandatory user review/edit/use step before the description enters `EmergencyClassifier`
- Local photo storage associated with the reporting device's Room packet record
- Room migration 1 → 2 that preserves existing emergency packets

## Image-analysis limitation

The bundled general-purpose ML Kit model labels visible objects and scenes; it is not a specialized disaster-severity model. ResQ creates conservative text from confident labels and asks the user to verify or edit it. It never automatically invents injuries, victim counts, causes, exact locations, priority, or a claim that the user is trapped. If analysis fails, the photo remains local and typed reporting continues normally.

## Offline route limitation

PMTiles contains display tiles, not a routable road graph. ResQ therefore shows the user, destination, distance, and a straight directional line. It does not claim to provide turn-by-turn navigation. Full offline road routing would require a separate routing graph/engine in a later version.

The map now displays color-coded hospitals and clinics, fire stations, police stations, emergency supplies, and shelters/support points. Rescue POIs work offline, can be tapped for their name/category/coordinates, and the location button centres the map on the phone's current position. Most POIs appear only after zooming to street/city level.

An older zoom-14 archive can show hospitals, but detailed services such as fire and police stations may not be stored in it. Generate a zoom-15 archive for the full rescue layer.

## What was added in 7.2

- Offline rescue-service POI layers with a clear color legend
- Tap-to-inspect rescue place details
- GPS-centred street-level map view
- Existing imported map remains usable; zoom 15 is recommended for complete POI coverage

## What was added in 7.1

- Real pan-and-zoom vector map rendered with MapLibre Android 13.6.1
- Local `karnataka.pmtiles` import with a strict 1 GB maximum
- Map file stored in ResQ's app-specific storage and available without internet
- Roads, water, land use, buildings and administrative boundaries
- All Room-backed ResQ SOS/medical/hazard/safe/rescue markers over the real map
- No API key and no Google Maps billing account
- Original schematic map remains available before import

## Create `karnataka.pmtiles` on Windows

1. Download the Windows `pmtiles.exe` CLI from the official Protomaps/go-pmtiles GitHub Releases page.
2. Visit `https://maps.protomaps.com/builds` and copy the URL of a recent Version 4 daily `.pmtiles` build.
3. In the folder containing `pmtiles.exe`, open PowerShell and run:

```powershell
.\pmtiles.exe extract "PASTE_DAILY_BUILD_URL_HERE" karnataka.pmtiles --bbox=74.05,11.50,78.60,18.80 --maxzoom=15
```

The bounding box covers Karnataka with a small safety margin. Zoom 15 is required for detailed fire, police, clinic, shelter, and supply POIs. If the result exceeds the app's 1 GB limit, keep the existing zoom-14 file for hospitals and major places, or make a smaller district/city archive at zoom 15. Each additional zoom level can substantially increase archive size.

4. Verify the archive:

```powershell
.\pmtiles.exe verify karnataka.pmtiles
```

5. Copy `karnataka.pmtiles` to the phone with USB, Google Drive, or another file-transfer method.
6. In ResQ open **Map** > **Import Karnataka Map**, select the file and wait for 100%.
7. Turn off Wi-Fi/mobile data and reopen Map to prove it is offline.

The PMTiles basemap is distributed under the ODbL Produced Work terms and requires OpenStreetMap attribution. ResQ displays attribution under the map.

## Milestone 7 added

- Fully offline, code-rendered map with no map API or internet dependency
- Persisted Room markers for SOS, hazard, safe zone, medical, and rescue locations
- Emergency packets automatically become tappable map markers
- Marker category filters and saved detail cards
- Current GPS location used as the local map center
- Built-in demonstration markers saved near the current location
- Deterministic coordinate projection unit tests

## Milestone 6 retained

- Deterministic local emergency text classification
- FLOOD, FIRE, MEDICAL, BLOCKED ROAD, DAMAGED BRIDGE, SOS, and manual OTHER fallback
- CRITICAL, URGENT, and NORMAL prioritization
- Visible five-stage AI analysis screen
- Actual classifier explanation and matched keywords
- Android on-device speech recognition on supported Android 12+ devices
- Offline-preferred system recognition fallback
- Typed text always remains available
- Unit tests for the required flood/trapped demo sentence and fallback rules

No cloud AI call is made by the classifier. No model is trained. The rule set is deliberately transparent and dependable for the hackathon demo.

## Install

1. Extract `ResQ_0_8_0_Live_Rescue_Android.zip`.
2. Open its inner `resq-android` folder in Android Studio.
3. Use JVM 21 and sync Gradle.
4. Install the same build on the test phones.
5. Preserve Milestone 4 as the stable Bluetooth baseline and Milestone 5 as the Wi-Fi/decision baseline.

Do not uninstall the existing ResQ application. Version code 10 upgrades versions 7.1/7.2 and Room migration 1 → 2 keeps the stored data. The already imported PMTiles file remains in app storage when the upgrade is signed with the same Android Studio debug key.

## Version 0.8.0 verification

### Live location

1. Grant location permission and enable GPS.
2. Open **Map** and confirm the legend shows **GPS: live**.
3. Move outdoors and confirm the user marker updates without repeatedly tapping the button.
4. Leave Map and confirm live tracking stops.
5. Create a report and confirm its packet coordinates use the latest fix.

### Nearby services and path

1. Import the zoom-15 Karnataka PMTiles archive.
2. Open **Map** and tap **Find Nearby**.
3. Choose Hospital / Clinic, Fire Station, Police Station, Pharmacy, Fuel / Water, or Shelter / Support.
4. Confirm up to five visible nearby facilities are sorted by straight-line distance.
5. Select one and tap **Show offline path**.
6. Confirm the user marker, destination marker, distance, and red directional line appear.
7. Confirm the straight-line limitation is displayed.

### Emergency photo

1. Open **Report Emergency**.
2. Tap **Take Photo** or **Choose Image**.
3. Wait for on-device analysis.
4. Review the detected labels and suggested description.
5. Tap **Edit** if needed, then **Use Description**.
6. Confirm the text appears in the normal description field.
7. Tap **Analyze Emergency** and confirm the existing five-stage classifier runs.
8. Create the packet and confirm Messages shows **Photo saved locally on reporting device**.

### Offline regression

1. Disable mobile data and Wi-Fi.
2. Repeat map, photo, text-classification, Room storage, Bluetooth forwarding, and Rescue Mode tests.
3. Confirm failure of photo analysis never prevents a manually typed emergency report.

## Required Milestone 7 demo test

1. Open **Map** from Home or the bottom navigation.
2. Tap the location button and grant location permission.
3. Tap the add-marker button once.
4. Confirm Safe Zone, Medical Point, Flooded Road, and Rescue Point appear.
5. Tap each marker and verify its details, coordinates, source, and saved time.
6. Turn off internet and reopen the app; confirm the markers remain available.
7. Create an SOS or analyzed emergency packet and return to Map.
8. Confirm the new emergency appears as an SOS/medical marker.

## Milestone 6 regression test

1. Enable GPS and open **Report Emergency**.
2. Select Flood (this remains the manual fallback).
3. Tap **Speak** and grant microphone permission.
4. Say: `There are people trapped near the flooded road.`
5. Confirm the transcript appears in the text box.
6. Tap **Analyze Emergency**.
7. Watch all five stages complete.
8. Confirm the result is:

```text
Type: FLOOD
Priority: CRITICAL
```

9. Tap **Create Emergency Packet**.
10. Open Messages and confirm the stored packet contains the transcript, GPS, timestamp, FLOOD type and CRITICAL priority.
11. Send the packet through the verified Bluetooth A -> B -> C/Rescue path.

## Typed fallback test

1. Disable internet/mobile data.
2. Type the same sentence manually.
3. Analyze and create the packet.
4. Verify the result is still FLOOD + CRITICAL.

This proves classification is local and speech failure cannot block emergency reporting.

## Additional classification tests

| Input | Expected type | Expected priority |
| --- | --- | --- |
| `Smoke and fire near a house` | FIRE | URGENT |
| `Person is unconscious and needs medical help` | MEDICAL | CRITICAL |
| `Road blocked by landslide and debris` | BLOCKED ROAD | URGENT |
| `Bridge collapse, people cannot escape` | DAMAGED BRIDGE | CRITICAL |
| `Minor road issue requires inspection` with Other selected | OTHER | NORMAL |

## Speech behavior

- Android 12/API 31+ devices use `createOnDeviceSpeechRecognizer()` when an on-device service is available.
- Otherwise ResQ uses the system recognizer with offline preference requested.
- Some phones require downloading an offline language pack in Google voice typing settings.
- If speech reports an unavailable model/service, type the emergency and continue through the identical classifier and packet pipeline.

## Important source paths

```text
app/src/main/java/com/resq/
  ai/classifier/EmergencyClassifier.kt
  ai/speech/SpeechInputManager.kt
  ui/ai/AiAnalysisScreen.kt
  ui/report/ReportScreen.kt
  ui/ResQViewModel.kt

app/src/test/java/com/resq/ai/classifier/
  EmergencyClassifierTest.kt
```

## Troubleshooting

- **Microphone denied:** Android Settings > Apps > ResQ > Permissions > Microphone > Allow.
- **Offline speech unavailable:** install/download the phone's offline English speech pack, or use typed text.
- **Speech transcription is inaccurate:** edit the transcript before tapping Analyze.
- **No GPS:** tap Get Current Location; packet creation remains disabled without a location.
- **Analysis result differs:** check exact wording and matched-keyword line; select the intended type as manual fallback.
- **Classifier test fails after editing rules:** restore deterministic precedence: critical phrases first, then urgent phrases, then normal.
- **Gradle JVM error:** use JVM 21, not JVM 25.

## Important source paths

```text
app/src/main/java/com/resq/
  map/OfflineMapProjector.kt
  ui/map/OfflineMapScreen.kt
  data/model/SupportEntities.kt
  data/db/SupportDao.kt
  ui/ResQViewModel.kt

app/src/test/java/com/resq/map/
  OfflineMapProjectorTest.kt
```

The map intentionally uses a schematic offline disaster layout instead of online map tiles. This guarantees the demonstration still works during a network outage.
