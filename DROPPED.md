# Dropped

Ideas decided against, with the reason. Any of these can come back if its reason stops being true.

- **iOS, browser and desktop support.** Daniel only plays on Android, so other platforms aren't worth the work. (2026-10-24)
- **Private servers on the modern client (retail or 1.14 emulators).** These clients must be patched to accept a non-Blizzard server, which Arctium does only for the x64 `Wow.exe`, not the ARM64 client this app runs. Comes back if someone patches the ARM64 client. (2026-10-24)
- **WineD3D (D3D9) renderer for 3.3.5a.** It didn't run on Daniel's Odin 3, so the option was removed. DXVK is the default and OpenGL stays as a fallback (about 12 fps there). Comes back if a newer Wine or Box64 makes it work. Saved WineD3D choices fall back to DXVK. (2026-10-24)
- **Private servers on 1.12.1 and 2.4.3 for now.** Daniel picked 3.3.5a for the first spike since it covers the most servers. Comes back once 3.3.5a works, if he plays elsewhere. (2026-10-24)
- **Frame rate cap for 3.3.5a.** A steady 30 or 40 fps through DXVK's `d3d9.maxFrameRate`, for smoothness and battery. Daniel passed on it. (2026-10-24)
- **A shared CurseForge proxy.** Upstream's `cf.yorudev.com` was the default, and running one for everyone was the other option. Daniel wants each player to bring their own key or proxy, so no searches pass through a server he doesn't run and no shared key's quota or terms are his to look after. Comes back if he decides to host one. (2026-10-24)
- **Back up WTF and SavedVariables before addon changes.** A copy plus a restore button, so ConsolePort setups and keybinds survive a bad install. Daniel passed on it. (2026-10-24)
- **Enter world automatically on 3.3.5a.** Character select pressing Enter World on the last character after auto login. Daniel said it isn't needed. (2026-10-27)

- **Test builds** (a release-signed APK published as a `test-build` pre-release, with its own in-app update channel). Daniel, 2026-10-24: stop with test builds and ship full releases instead. Removed in 0.5.1, along with the old releases.
