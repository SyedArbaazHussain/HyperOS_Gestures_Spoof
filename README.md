# 🚀 HyperOS Gestures Spoof (HGS)

**A High-Performance Framework Utility for HyperOS 3 Launcher Synchronization**


[![Build and Release Signed APK](https://github.com/SyedArbaazHussain/HyperOS_Gestures_Spoof/actions/workflows/build.yml/badge.svg)](https://github.com/SyedArbaazHussain/HyperOS_Gestures_Spoof/actions)

![Platform](https://img.shields.io/badge/Platform-Android%2014%2B%20(HyperOS%203)-red.svg)

![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)


---


**HyperOS Gestures Spoof** is a professional-grade LSPosed module developed by **Syed Arbaaz Hussain and Gemini AI (see disclaimer)**. It is engineered to bypass the restrictive gesture navigation limitations inherent in HyperOS 3. Unlike traditional "launcher switchers" that struggle with system-level resets, HGS implements a sophisticated **Persistent Veto System** at the Android Framework level to maintain fluid navigation regardless of the active launcher.


---


## 🧠 The Architecture: "Persistent Veto"


In HyperOS 3, the system aggressively monitors the `CATEGORY_HOME` intent handler. If a third-party package is detected, `DisplayPolicy` forcefully resets the `force_fsg_nav_bar` global setting to `0` (Buttons). 


HGS solves this through a brilliant three-tier interception strategy:


### 1. Framework-Level Veto
The module hooks into `android.provider.Settings.Global` within the System Server. Any attempt by the MIUI Framework to write a value other than `1` (Gestures) to the navigation flag is **vetoed** in real-time. The framework is tricked into believing the write operation succeeded, while the underlying system remains locked in Gesture mode.


### 2. Process Redirection (Stealth Mode)
To avoid triggering MIUI's defensive integrity checks, HGS encourages keeping `com.miui.home` as the system default launcher. The module then hooks the `onResume` cycle of the stock launcher and instantly redirects the intent to your chosen third-party launcher. This preserves native system animations and keeps the OS "unaware" of the change.


### 3. WindowManager Enforcement
We patch the low-level `com.android.server.wm.DisplayPolicy` to forcefully suppress the `mForceShowNavigationBar` and `mHasNavigationBar` flags. This ensures that even during a "fallback" scenario, the 3-button navigation bar is physically prevented from rendering on the display.


---


## ✨ Premium Features


* **Material You & AMOLED Hybrid UI:**
    * **Light Mode:** Full integration with Android 14 Dynamic Color (wallpaper-based tonal palettes).
    * **Dark Mode:** Pure AMOLED Black (#000000) implementation for maximum OLED battery efficiency.

* **Real-Time Diagnostics & Logs:** A dedicated navigation tab providing a live "Heartbeat" status check and operational logs to verify framework hooks are active and enforced.

* **Ultra-Lightweight Footprint:** Optimized with R8/ProGuard post-processing, resulting in a highly efficient binary under 200KB.

* **Stealth Integration:** The app remains hidden from the launcher drawer to reduce clutter; settings are accessible exclusively via the "Settings" gear in the LSPosed Manager.


---


## 🛠 Installation & Setup


1. **Requirements:**
    * HyperOS 3 (Android 14 or 15).
    * Root access (Magisk, KernelSU, or APatch).
    * LSPosed Framework (Zygisk version recommended).


2. **Steps:**
    * Download the latest APK from the [Releases](https://github.com/SyedArbaazHussain/HyperOS_Gestures_Spoof/releases) section.
    * Install the APK and enable the module in the **LSPosed Manager**.
    * **Crucial:** Set the module scope to include `Android System`, `System UI`, and `Miui Home`.
    * Open the module via LSPosed, select your desired launcher, and enjoy permanent gestures.


---


## 🛠 Development & Build
This project utilizes GitHub Actions for automated, secure CI/CD builds and signing.


    # To build locally using the Gradle wrapper:
    ./gradlew assembleRelease


To enable automated releases via GitHub, add your .jks file as a Base64 string to GitHub Secrets under SIGNING_KEY, along with KEY_ALIAS, KEY_PASSWORD, and KEYSTORE_PASSWORD.


---


## ⚠️ Disclaimer & AI Disclosure

**Use at your own risk.** This project was built **entirely through iterative prompting and log analysis provided to Gemini AI**. 

* **Experimental Nature:** As a developer, I cannot guarantee that the code provided is a strictly accurate or "official" implementation of a system fix. It should be treated as a temporary workaround based on the current observed state of HyperOS 3.
* **No Liability:** I am not responsible for any device harm, software instability, bootloops, or data loss that may occur as a result of using this module. 
* **Validation:** Users are encouraged to review the source code and logs provided in the app to verify functionality on their specific device build.


---


## 📜 License


Copyright (C) 2026 Syed Arbaaz Hussain.  

Distributed under the **GNU General Public License v3.0**. See the LICENSE file for full legal text.