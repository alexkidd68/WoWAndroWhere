# Changelog

Releases up to 0.4.0 are upstream's (YoruDev-Ryland/WoWForeverywhere), which this fork started from.

## Unreleased

- **The app is now called WoW AndroWhere.** The new name shows on the home screen, in the app drawer and in Settings, in every language. The app ID is unchanged, so this installs as a normal update and keeps your settings, saved login, game files and addons. The release download is now **WoW-AndroWhere.apk**.
- **Private servers (3.3.5a).** Settings → Game version → Private server (3.3.5a) runs your own Wrath of the Lich King 3.3.5a client. Choose its folder and enter your server's realmlist in **Client and server**, then press Play. The home screen, Play and Addons work for it like for the other versions. It's experimental.
- **3.3.5a addons.** Addons lists WoWInterface addons made for 3.3, and **Install from link** takes a GitHub repository even when it has no releases.
- **Server password.** Save your private server password under **Server login**. On the game's login screen, press Back and choose **Type password** to enter it. It's stored encrypted on the device and never goes into the game folder.
- **Wine logs for private servers.** After a private server game closes, the app asks whether to share its log, which helps track down launch problems. Nothing is sent unless you choose where to share it.
- **Controller support on 3.3.5a.** The 3.3.5a client can't use a controller on its own. Addons now offers **Install ConsolePortLK**, and while it's installed the app maps your controller to the keys the addon expects. The first time the game starts with it, choose the Xbox layout and press each button once when the addon asks.

## 0.4.1

The first release from this fork.

- **App updates come from WoW AndroWhere.** The app checks this fork's releases for updates, and Settings → Source code opens it. If your current copy came from the upstream WoW Foreverywhere releases, uninstall it before installing this one. Your game files and addons stay on your storage; the saved login and app settings need to be set again.

## 0.4.0

- **Other WoW versions (experimental).** Settings → Game version lets you pick Classic Era, Burning Crusade Anniversary, Mists of Pandaria Classic or Retail. The whole app then installs, updates, starts and finds addons for that version. Each version keeps its own install and addons, and you can switch back to WoW Forever without losing anything. These versions may run poorly or not start, and automatic sign-in may not work on all of them.
- Settings now shows CurseForge as on.
- Game icons no longer go missing when several load at once.

## 0.3.0

- **CurseForge in the addon browser.** Search, browse, install and update CurseForge addons, with Forever files picked first. No account or key needed. Addons whose authors don't allow third-party downloads show **Open on website** instead.
- Addons land in the right categories more reliably (raid frames under Unit frames, single professions under Professions).
- If the app stops responding, it saves details about what it was doing to help track down reports.

## 0.2.1

- Saving your Battle.net login no longer crashes the app on Android 15 devices without a screen lock. If the device refuses, the app shows a message instead.
- Removed an unused system permission (`READ_LOGS`).

## 0.2.0

The first release: WoW Forever on Snapdragon Android handhelds without a PC.

- Installs the game from Blizzard's CDN to internal storage or an SD card, with pause, resume, repair and background downloading.
- Finds installs already on the device, including ones copied from a PC.
- Runs the WoW Forever beta and the launch version, with a switch that reuses your game data.
- Addon manager for WoWInterface and a curated GitHub catalog, with Forever and Classic builds marked.
- Optional saved Battle.net login, sealed with the Android Keystore and handed to the game for a few seconds only.
- Controller-first home screen with WoW Forever artwork.
