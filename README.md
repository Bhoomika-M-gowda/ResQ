ResQ — Live Rescue Intelligence

ResQ is an offline-first Android emergency-response application designed to support emergency reporting, local classification, GPS-based rescue assistance, offline maps, and device-to-device emergency communication when internet connectivity is unavailable or unreliable.

Platform: Android
Version: 0.8.1
Status: Hackathon / Development Build

1. Context & Overview

Problem

During emergencies, internet connectivity may be unavailable or unreliable. ResQ keeps essential reporting and rescue-support features available locally.

Core Features

Emergency reporting using text, speech, and photos

Local emergency classification and priority detection

GPS-based live location

Offline MapLibre + PMTiles maps

Nearby hospitals, clinics, fire stations, police stations, pharmacies, fuel/water points, and shelters

Offline distance calculation and directional path

On-device ML Kit image labeling

Local Room database for emergency packets and support data

Bluetooth-based emergency packet forwarding

Rescue Mode and emergency map markers

Typed-text fallback when speech recognition is unavailable

Current Highlights

YOU • This device location marker

Automatic map centering on the first GPS fix

Nearby rescue-service search

Offline emergency-photo analysis

User review before photo-generated descriptions are used

Offline emergency classification without cloud AI

2. Badges & Demo

CI, test-coverage, and code-quality badges can be added after the corresponding workflows are configured.

Demo Screenshots / Video

Add screenshots or a demo video showing:

Emergency reporting

AI analysis

Offline map

Nearby rescue services

Emergency photo analysis

Emergency packet forwarding

3. Architecture & System Design

High-Level Architecture

                ┌──────────────────┐
                │    ResQ Android  │
                └────────┬─────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
Emergency Input    GPS & Map       Local Storage
Text/Speech/Photo  MapLibre/PMTiles    Room
│                │                │
▼                ▼                │
Emergency Classifier    │                │
Type + Priority         │                │
│                │                │
└────────┬───────┴────────────────┘
▼
Emergency Packet
│
┌──────┴──────┐
▼             ▼
Bluetooth      Rescue Mode
Forwarding

Emergency Flow

Text / Speech / Photo
↓
Input Processing
↓
Local Classification
↓
User Review
↓
GPS + Timestamp
↓
Emergency Packet
↓
Room Storage
↓
Bluetooth / Rescue Mode

Main Technologies

Technology

Purpose

Kotlin

Android development

MapLibre Android 13.6.1

Offline vector maps

PMTiles

Offline map data

Room

Local persistence

Fused Location

GPS tracking

ML Kit

On-device image labeling

Android Speech Recognition

Voice input

Bluetooth

Emergency packet forwarding

4. Installation & Configuration

Prerequisites

Android Studio

JVM 21

Android device(s) for testing

Location/GPS permission

Microphone permission for speech input

PMTiles archive for the complete offline-map experience

The existing project documentation does not specify exact Android Studio, Gradle, SDK, or hardware versions.

Installation

Open the resq-android project in Android Studio.

Configure JVM 21.

Sync Gradle.

Build and install the application on the test device.

Offline Karnataka Map

Download pmtiles.exe and a suitable Version 4 daily PMTiles build.

.\pmtiles.exe extract "PASTE_DAILY_BUILD_URL_HERE" karnataka.pmtiles --bbox=74.05,11.50,78.60,18.80 --maxzoom=15

Verify:

.\pmtiles.exe verify karnataka.pmtiles

Then:

Map → Import Karnataka Map

For complete rescue-service POI coverage, zoom 15 is recommended. The application has a documented 1 GB map-file limit.

Environment Variables

No .env variables are documented in the existing project README.

5. Developer Experience & Quality Control

Important Source Paths

app/src/main/java/com/resq/
├── ai/classifier/EmergencyClassifier.kt
├── ai/speech/SpeechInputManager.kt
├── ui/ai/AiAnalysisScreen.kt
├── ui/report/ReportScreen.kt
├── ui/ResQViewModel.kt
├── map/OfflineMapProjector.kt
├── ui/map/OfflineMapScreen.kt
├── data/model/SupportEntities.kt
└── data/db/SupportDao.kt

app/src/test/java/com/resq/
├── ai/classifier/EmergencyClassifierTest.kt
└── map/OfflineMapProjectorTest.kt

Example Emergency Classification

Input:

There are people trapped near the flooded road.

Expected:

Type: FLOOD
Priority: CRITICAL

Other supported classifications include:

Emergency

Priority

FIRE

URGENT

MEDICAL

CRITICAL

BLOCKED ROAD

URGENT

DAMAGED BRIDGE

CRITICAL

OTHER

NORMAL

Testing

The project includes unit tests for emergency classification and offline map coordinate projection.

Typical Gradle commands should be verified against the project's actual configuration before being added as official commands:

./gradlew test
./gradlew lint

6. Reliability, Performance & Security

Offline Reliability

ResQ is designed so that core emergency functions continue working without internet access:

Emergency classification runs locally.

Typed reporting works when speech fails.

Maps can be stored locally using PMTiles.

Emergency packets are stored using Room.

Bluetooth supports local packet forwarding.

Photo analysis uses on-device ML Kit.

Known Limitations

Area

Limitation

Offline routing

PMTiles is not a road-routing graph

Photo analysis

ML Kit is not a specialized disaster-severity model

Speech

Offline speech availability depends on the device

Map size

PMTiles archive has a documented 1 GB application limit

GPS

Emergency packet creation requires a location

Classification

Uses deterministic local rules rather than a trained cloud model

Offline Path

ResQ displays the user, destination, distance, and a straight directional line. It does not provide turn-by-turn road navigation.

Image Analysis

ML Kit labels visible objects/scenes. ResQ creates conservative descriptions and requires the user to review or edit them before they are used. It does not automatically invent injuries, victim counts, causes, exact locations, or claims that a user is trapped.

Performance

The existing README does not provide measured latency, throughput, memory, battery, or benchmark results.

Security Reporting

A private vulnerability-reporting procedure is not documented yet and should be added before public release.

7. Troubleshooting

Problem

Solution

Microphone denied

Allow Microphone permission in Android Settings

Offline speech unavailable

Download an offline language pack or use typed text

Inaccurate speech

Edit the transcript before analysis

No GPS

Enable GPS and use Get Current Location

POIs missing

Use a suitable zoom-15 PMTiles archive

Gradle JVM error

Use JVM 21, not JVM 25

Classification differs

Check wording and use the manual type fallback

8. Governance & License

Map Data

The PMTiles basemap requires OpenStreetMap attribution and is distributed under the ODbL Produced Work terms.

Project License

The existing README does not specify a source-code license. Add a LICENSE file and contribution guidelines before public release.

Contribution Guidelines

Add project-specific rules for:

Branching

Pull requests

Code style

Testing

Issue reporting

External assets and datasets

Repository

https://github.com/Bhoomika-M-gowda/ResQ