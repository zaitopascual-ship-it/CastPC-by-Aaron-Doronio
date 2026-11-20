# Cast2PC by Aaron Doronio

A lightweight Android app that streams your phone screen **and game audio** to your PC over Wi-Fi. Built for streaming mobile games (like COD: Mobile) to OBS, with no cables.

```
Phone (Cast2PC)  --Wi-Fi-->  MediaMTX (PC)  -->  VLC / OBS
```

## Features

- Wireless screen cast with audio
- Landscape or portrait cast, picked in the app
- Runs in the background while you play
- One-click launcher on the PC
- Hardware encoding, no OpenGL, no extra UI libraries
- Remembers your PC's IP address

---

# Complete Beginner's Guide

**No experience needed.** Follow the steps in order, and don't skip any. Each step tells you what you should see when it works.

## Contents

1. [Words you'll see](#words-youll-see)
2. [Before you start: checklist](#before-you-start-checklist)
3. [Part 1: Install the app on your phone](#part-1-install-the-app-on-your-phone)
4. [Part 2: Set up your PC (only once)](#part-2-set-up-your-pc-only-once)
5. [Part 3: Start casting](#part-3-start-casting-do-this-every-time)
6. [Part 4: Stop casting](#part-4-stop-casting)
7. [Part 5: Use it in OBS](#part-5-use-it-in-obs-for-streaming-or-recording)
8. [Troubleshooting](#troubleshooting)
9. [Known limitations](#known-limitations)
10. [Build from source](#build-from-source)

---

## Words you'll see

| Word | What it means |
|---|---|
| **APK** | The installer file for an Android app. |
| **Wi-Fi network** | The Wi-Fi name your phone and PC are connected to. They must be on the **same one**. |
| **IP address** | Your PC's "address" on the Wi-Fi, like `192.168.1.10`. The phone needs it to know where to send the video. |
| **MediaMTX** | A small free program on your PC that receives the video from your phone. |
| **VLC** | A free video player that shows your phone screen on the PC. |
| **OBS** | A free program for streaming or recording. Only needed if you want to stream or record. |
| **Launcher** | `CastPC-Start.bat`, a file that starts MediaMTX and VLC for you with one double-click. |

---

## Before you start: checklist

Tick every box before you begin.

- [ ] My phone is **Android 10 or newer**. (Settings > About phone > Android version)
- [ ] My PC runs **Windows**.
- [ ] My phone and my PC are connected to the **same Wi-Fi**.
- [ ] I have about **10 minutes** for the first-time setup.

---

## Part 1: Install the app on your phone

You only do this once.

### Step 1.1: Download the app

1. On your **phone**, open this GitHub page in your browser.
2. On the right side, tap **Releases**, then tap the newest release (the top one).
3. Scroll down to **Assets**.
4. Tap **`Cast2PC.apk`**. Your phone downloads the file.
5. If your browser says "this type of file can harm your device", tap **Download anyway** or **OK**. The file is safe, it's your own app.

### Step 1.2: Install it

1. When the download finishes, tap **Open** on the download message. You can also open your **Files** or **Downloads** app and tap `Cast2PC.apk`.
2. Android may say **"For your security, your phone isn't allowed to install unknown apps from this source."** This is normal, because the app isn't on the Play Store.
   - Tap **Settings** on that message.
   - Turn on **Allow from this source**.
   - Press the **Back** button to return.
3. Tap **Install**.
4. If **Play Protect** shows a warning like "unknown developer", tap **More details**, then **Install anyway**.
5. When it says **App installed**, tap **Open**.

**You should see:** a dark blue screen with the Cast2PC logo, a box for the **PC IP address**, and a big blue **Start casting** button.

### Step 1.3: Allow the permissions

When you first open the app, your phone asks for permissions. Allow them, or the app won't work properly.

1. **Record audio / microphone:** tap **While using the app** (or **Allow**). The app needs this to capture the sound from your games.
2. **Notifications:** tap **Allow**. This shows a small notification while casting is running.

If you tapped "Don't allow" by accident, go to your phone's **Settings > Apps > Cast2PC > Permissions** and turn them on.

---

## Part 2: Set up your PC (only once)

### Step 2.1: Download MediaMTX

1. On your **PC**, open a browser and go to: **https://github.com/bluenviron/mediamtx/releases**
2. Find the newest release at the top and scroll down to **Assets**.
3. Click the file that ends with **`windows_amd64.zip`**. For example: `mediamtx_v1.21.4_windows_amd64.zip`. Your PC downloads it.

### Step 2.2: Unzip it

1. Open your **Downloads** folder and find the zip file.
2. **Right-click** the zip file and click **Extract All...**
3. Click **Extract**. A new folder opens.

**You should see:** a folder with files named `mediamtx.exe` and `mediamtx.yml` in it. Remember where this folder is. This is your **MediaMTX folder**.

> **Important:** don't delete or move this folder later, or the launcher won't find it.

### Step 2.3: Install VLC

1. Go to **https://www.videolan.org** and click the big orange **Download VLC** button.
2. Open the downloaded file and click **Next** through the installer until it finishes.

If you already have VLC, skip this step.

### Step 2.4: Download the launcher

1. On this GitHub page, click the file **`CastPC-Start.bat`** in the file list.
2. Click the **download icon** (down arrow) at the top right of the file view.
3. Find the downloaded file in your **Downloads** folder.
4. **Copy it into your MediaMTX folder** (the one from Step 2.2), so it sits right next to `mediamtx.exe`. You can drag and drop it.

If Windows or your browser says the file might be harmful, click **Keep** (or **Keep anyway**). It's only a small text file that starts the two programs.

### Step 2.5: Run it once and allow the firewall

1. In your MediaMTX folder, **double-click `CastPC-Start.bat`**.
2. If Windows shows a blue **"Windows protected your PC"** window, click **More info**, then **Run anyway**.
3. A black window opens with text. Windows Firewall may pop up a question:
   - Make sure **Private networks** is ticked.
   - Click **Allow access**.
4. The black window should show your PC's IP address and the message **"Waiting for your phone..."**.

You're done with the setup. Close the black window for now.

### Step 2.6 (optional): Put a shortcut on your Desktop

1. Right-click `CastPC-Start.bat`.
2. Click **Show more options** if you see it, then **Send to > Desktop (create shortcut)**.

Now you can start everything from your Desktop.

---

## Part 3: Start casting (do this every time)

### Step 3.1: Start the launcher on the PC

1. Double-click **`CastPC-Start.bat`** (or the Desktop shortcut).
2. A black window opens. **Leave it open the whole time you stream.**
3. Look for the lines that say **IPv4 Address**. The number next to it is your **PC's IP address**. It looks like this:

```
IPv4 Address. . . . . . . . . . . : 192.168.1.10
```

4. Write that number down or keep the window visible. Use the one that starts with **`192.168`**. If you see several lines, pick the one that starts with `192.168`.

**If the window doesn't show the IP, find it yourself:**
1. Press the **Windows key + R** on your keyboard.
2. Type `cmd` and press **Enter**.
3. Type `ipconfig` and press **Enter**.
4. Find **IPv4 Address** under **Wireless LAN adapter Wi-Fi** and use that number.

### Step 3.2: Make sure the phone is on the same Wi-Fi

1. On your phone, open **Settings > Wi-Fi**.
2. Check that the connected Wi-Fi name is the **same** one your PC uses.

> If your phone is on mobile data, or on a different Wi-Fi (for example a guest network), it **will not work**.

### Step 3.3: Open the app and enter the IP

1. Open **Cast2PC** on your phone.
2. Tap the **PC IP address** box.
3. Type the IP address from your PC, **exactly**, with the dots. Example: `192.168.1.10`
4. The app remembers this number, so next time it's already filled in. Only change it if your PC's IP changes.

### Step 3.4: Pick the orientation

Under **Cast orientation**, tap one:

- **Landscape:** choose this for most games, including **COD: Mobile**. The phone is held sideways.
- **Portrait:** choose this if your game or app is held upright.

> **Choose the one that matches how your game looks.** You can't change it while casting. To switch, tap **Stop casting**, change it, then start again.

### Step 3.5: Start

1. Tap the big **Start casting** button.
2. Your phone asks: **"Start recording or casting with Cast2PC?"** (the wording can differ a little between phones).
3. Tap **Start now** (or **Allow**).
4. The button turns **red** and says **Stop casting**. The status shows **"Casting. Switch to your game."**
5. A short message **"Connected - casting"** may pop up.

### Step 3.6: Watch it on your PC

1. **VLC opens by itself** on your PC after a second or two, showing your phone screen.
2. Press the **Home** button on your phone and open your game.
3. Your PC now shows your game, with the sound.

**You should see:** your phone's screen live on the PC, and hear the game's sound from your PC speakers.

> **If VLC doesn't open by itself,** open VLC yourself, click **Media > Open Network Stream**, paste `rtsp://localhost:8554/cod`, and click **Play**.

---

## Part 4: Stop casting

1. Open **Cast2PC** on your phone.
2. Tap the red **Stop casting** button. It turns blue again and says **"Ready."**
3. On the PC, close VLC.
4. Close the **black window**. This turns off MediaMTX.

---

## Part 5: Use it in OBS (for streaming or recording)

Skip this if you only want to watch in VLC.

1. Start the launcher and start casting from your phone (Part 3).
2. Open **OBS**.
3. Look at the **Sources** box at the bottom of OBS. Click the **+** button there.
4. Click **Media Source**, then **OK** (you can keep the default name), then **OK** again.
5. In the window that opens:
   - **Untick** "Local File".
   - In the **Input** box, type exactly: `rtsp://localhost:8554/cod`
   - Set **Network Buffering** to **0 MB**.
   - **Tick** "Use hardware decoding when available".
6. Click **OK**. Your phone screen appears in OBS.
7. Click the video in the OBS preview once, and press **Ctrl+F** on your keyboard. This fits the video to the screen.

**Sound:** the game sound comes through this same Media Source. You should see its bar move in the **Audio Mixer**.

**If your voice doesn't match the video** (the video is a bit late):
1. In the **Audio Mixer**, click the **three dots** next to your microphone.
2. Click **Advanced Audio Properties**.
3. Find your microphone and put a number like `1500` in **Sync Offset (ms)**. Raise or lower it until the voice matches the video.

**Black bars on the sides?** That's normal, because your phone screen is wider than a normal video. To remove them, go to **Settings > Video** in OBS and set the **Base (Canvas) Resolution** to `1600x720`, then press **Ctrl+F** again.

---

## Troubleshooting

### "Connection failed" shows on the phone

Try these in order:
1. Is the **black window open** on the PC? If not, start the launcher first.
2. Is the **IP address** typed correctly? Check every number and dot.
3. Are the phone and PC on the **same Wi-Fi**? Check Step 3.2.
4. Did you click **Allow access** in the Windows Firewall question? If you're not sure, close the launcher, run it again, and check the box for **Private networks**.
5. Is there **another MediaMTX window** open? Close all of them and start the launcher again. Only one can run at a time.

### The app says casting, but nothing shows on the PC

1. Make sure the black window is open and didn't show an error.
2. Open VLC yourself: **Media > Open Network Stream**, paste `rtsp://localhost:8554/cod`, click **Play**.
3. Stop casting and start again.

### I can see the video but there's no sound

1. Check that you allowed the **microphone permission** for Cast2PC (Phone Settings > Apps > Cast2PC > Permissions).
2. **Some games block audio capture on purpose,** and no app can get around that without rooting the phone. Test with other apps (YouTube, for example) to see if sound works there.
3. Check that your PC volume isn't muted.

### The picture is blurry, blocky, or stutters

1. Move closer to your Wi-Fi router.
2. Use the **5 GHz** Wi-Fi if your router has both. It's often named like `MyWifi-5G`.
3. Close other apps and downloads that use the internet.

### The video is late by 2 to 3 seconds

That's normal for this kind of stream. It doesn't affect how you play, because you play on the phone. Only the PC picture is delayed.

### It worked yesterday but not today

Your PC's IP address probably changed (this happens after a restart or a Wi-Fi reconnect). Start the launcher, read the new IP from the black window, and type it into the app.

### The launcher says "mediamtx.exe was not found"

`CastPC-Start.bat` is not in the same folder as `mediamtx.exe`. Move it there (Step 2.4).

### The launcher says "VLC was not found"

Install VLC (Step 2.3), then run the launcher again.

---

## Quick checklist for every stream

1. Phone and PC on the same Wi-Fi.
2. Double-click `CastPC-Start.bat` on the PC. Read the IP.
3. Open Cast2PC, check the IP, pick the orientation.
4. Tap **Start casting** > **Start now**.
5. Open your game.
6. When done: tap **Stop casting**, then close the black window.

---

## Known limitations

- Game audio can be blocked. Android lets apps opt out of audio capture.
- Latency is around 2 to 3 seconds. You may need OBS Sync Offset to line up your mic with the video.
- Wi-Fi quality decides stream quality. Stay close to your router.

## Build from source

1. Open the project in Android Studio.
2. Let Gradle sync, connect your phone with USB debugging on, and press Run.

Resolution, frame rate, and bitrate can be changed in `StreamService.kt`.

## Built with

- [RootEncoder](https://github.com/pedroSG94/RootEncoder) for screen capture and RTMP streaming
- [MediaMTX](https://github.com/bluenviron/mediamtx) as the PC-side media server
