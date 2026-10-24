# TODO

Only work that's left. Shipped items move to DONE.md, rejected ones to DROPPED.md.

## Fork housekeeping

1. **Addon catalog link.** The GitHub addon catalog (`GitHubProvider.CATALOG_URL`) still points at upstream. It can stay on upstream's nightly build (no cost, but it's their branch) or the fork's `addon-catalog` workflow can publish its own.

## Private servers (3.3.5a)

Shipped as a game version (see DONE.md). What's left:

1. **3.3.5a addon list.** The browser only finds 3.3.5a addons on WoWInterface; most live in GitHub repos without releases, which install from a link today. Add a short, checked list of 3.3.5a repos (pfQuest, ElvUI, WeakAuras, DBM-Warmane style backports) to the catalog under a `wotlk335` key so they show up when browsing, installed from the default branch.

## 3.3.5a speed on the Odin 3

Logs off by default and a DXVK overlay shipped (see DONE.md). Next steps depend on what the overlay shows:

1. **If GPU load is low (CPU-bound, expected).** Compare DXVK, WineD3D and OpenGL on the same spot, then try a handheld preset for first boot of the 3.3.5a client (lower view distance, ground clutter, shadows off, full-screen glow off), written only when the key is missing so the player's choices stick.
2. **If GPU load is near 100%.** Lower the 3.3.5a render size below 1280x720 on Adreno 8xx, or try the Adreno 740 driver profile.

## Proposed by Claude, not agreed yet

- **Keyboard on the 3.3.5a login screen.** Addons can't run on the login, code and character screens, so typing there means Back, then Keyboard. Either open the keyboard by itself when the client starts, or map a controller button to it for the private server container.

- **Own the CurseForge proxy.** Release builds without a key send every CurseForge request to `cf.yorudev.com`, upstream's server. Either run `server/curseforge-proxy` with Daniel's own key and build with `-PCURSEFORGE_PROXY=...`, or accept upstream's server and say so in NOTES.md.
