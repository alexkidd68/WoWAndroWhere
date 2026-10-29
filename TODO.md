# TODO

Only work that's left. Shipped items move to DONE.md, rejected ones to DROPPED.md.

## Next: your own 3.3.5a server

Agreed with Daniel on 2026-10-29, after looking at Dad's MMO Lab (offline AzerothCore servers on the Steam Deck). One release, marked experimental, since it can only be tested on the Odin.

- **"Your own server" section on the Server screen.** Under Servers on private server profiles, two rows: On this device and On a PC. A server set up from either shows in the Servers list like any other ("Solo, this device" or "Solo, PC"), so Play, Controller Menus and auto login work unchanged. New code in `wow/solo` and its own screen file, with only a section call added to `LegacyServerScreen`.
- **Solo server on this device: the server.** AzerothCore 3.3.5a (no playerbots) and MariaDB, built for ARM64 Linux by a CI workflow from pinned tags and published as a release asset with a sha256. The app downloads them on first setup (about 300 MB, a guess), never in the APK. They run as child processes inside the app's existing Linux image (imagefs), kept alive by a foreground service while the game runs. That this works at all is the biggest unknown, so it's the first thing to try. Listens on 127.0.0.1 only.
- **Solo server on this device: map data.** AzerothCore's prebuilt client-data (wowgaming/client-data v20.0, `Data.zip`, 1.2 GB, 3.2 GB unpacked, sha256 `a3d4df635ae6c2c8f08052c32a79e0f806955150ad36b014a823dd08a32a4610`), checked before use. The app reads the zip's file list and fetches only the folders it needs with range requests, so mob pathing (mmaps, 2.19 GB) is a switch, on by default; without it the rest is about 1 GB. See NOTES.md for why this is downloaded and not extracted.
- **Solo server on this device: setup page.** Shows what will be downloaded and its size, free space left, one Set up button, then progress for download, check and the first world database import (about 15 to 30 minutes on the phone, a guess). After setup: status (Stopped, Starting, Running), Start and Stop, "GM commands for my account", and Remove server with the space it frees. The app creates the account through the server console and keeps its password in LoginVault, so the console is never shown.
- **Solo server on this device: Play.** Picking the solo server and pressing Play starts the server if it's stopped, waits for the world server's ready line, then launches the client with realmlist 127.0.0.1 and auto login. The server stops when the game exits. Measure fps with the server running against Warmane on the same spot, since the client is CPU-bound and the server takes some of it.
- **Solo server on a PC.** A page to type the PC's address, with Check connection (a TCP connect to port 3724) and a short how-to for Dad's MMO Lab's Windows installer, including setting the realm address to the PC's LAN IP. Saving adds it to the Servers list. No code runs on the PC from the app.

## Next: older private server versions

Daniel asked for these on 2026-10-29; a separate thread builds them.

- **Vanilla 1.12.1 (and Turtle WoW) and TBC 2.4.3 private servers.** Same 32-bit Wow.exe, D3D9 and realmlist path as 3.3.5a, so basic play is a new game version that reuses the legacy launch. Controller Menus, the login patch (Wow.exe offsets) and the addon catalog are 3.3.5a specific and each need their own port per client. Their solo server would be CMaNGOS Classic and TBC.
- **Cataclysm 4.3.4 private servers.** Still 32-bit with realmlist, so the same path, but a much heavier client on an already CPU-bound setup; expect lower fps than 3.3.5a (a guess). Try after 1.12.1 and 2.4.3.

## Parked

- **Mists of Pandaria 5.4.8 private servers: parked.** The 32-bit client is the heaviest under Box64 and the 64-bit one needs FEX x64, which isn't bundled. Official MoP Classic already runs natively. Revisit after Cataclysm shows how far emulation goes.
