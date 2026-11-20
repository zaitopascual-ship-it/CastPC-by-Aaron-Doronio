# Cast2PC by Aaron Doronio

A lightweight Android app that streams your phone screen **and game audio** to your PC over Wi-Fi. Built for streaming mobile games (like COD: Mobile) to OBS, with no cables.

## How it works

```
Phone (CastPC)  --RTMP over Wi-Fi-->  MediaMTX (PC)  -->  OBS / VLC
```

The app captures the screen and internal audio, encodes it with the phone's hardware encoder, and pushes it to a MediaMTX server running on your PC. OBS or VLC then reads the stream from MediaMTX.

## Features

- Wireless screen cast with audio
- Landscape or portrait cast, picked in the app
- Runs in the background while you play
- Hardware encoding, no OpenGL, no extra UI libraries
- Remembers your PC's IP address

## Requirements

- Android 10 or newer
- A PC on the same Wi-Fi network (5 GHz recommended)
- [MediaMTX](https://github.com/bluenviron/mediamtx/releases) on the PC
- OBS or VLC to watch the stream

## Setup

**On the PC**
1. Download MediaMTX, unzip it, and run `mediamtx.exe`. Allow it through the firewall on private networks.
2. Find your PC's IP address: press Win + R, run `cmd`, type `ipconfig`, and note the IPv4 address.

**On the phone**
1. Install the APK from the Releases page (allow "Install unknown apps" when asked).
2. Open CastPC, allow the microphone and notification permissions.
3. Enter your PC's IP, pick Landscape or Portrait, and tap **Start casting**.

**Watch the stream**
- **VLC:** Media > Open Network Stream > `rtsp://localhost:8554/cod`
- **OBS:** add a Media Source, uncheck "Local File", and set the input to `rtsp://localhost:8554/cod`. Set Network Buffering to 0.

## Known limitations

- **Game audio can be blocked.** Android lets apps opt out of audio capture. If a game's sound doesn't come through, the game is blocking it.
- **Latency is around 2-3 seconds.** This doesn't affect gameplay since you play on the phone, but you may need to use OBS Sync Offset to line up your mic with the video.
- Wi-Fi quality decides stream quality. Stay close to your router.

## Build from source

1. Open the project in Android Studio.
2. Let Gradle sync, connect your phone with USB debugging on, and press Run.

Video and audio resolution, frame rate, and bitrate can be changed in `StreamService.kt`.

## Built with

- [RootEncoder](https://github.com/pedroSG94/RootEncoder) for screen capture and RTMP streaming
- [MediaMTX](https://github.com/bluenviron/mediamtx) as the PC-side media server
