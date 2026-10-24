# TODO

Only work that's left. Shipped items move to DONE.md, rejected ones to DROPPED.md.

## Fork housekeeping

1. **Addon catalog link.** The GitHub addon catalog (`GitHubProvider.CATALOG_URL`) still points at upstream. It can stay on upstream's nightly build (no cost, but it's their branch) or the fork's `addon-catalog` workflow can publish its own.

## Controller support on 3.3.5a: ConsolePortLK

Unblocked: the 3.3.5a client reaches the world on Daniel's phone (Warmane, 2026-10-24). ConsolePortLK only targets 3.3.5a (`## Interface: 30300`); every official version the app runs already has native gamepad support.

1. **One-tap install.** With the 3.3.5a client selected, Addons shows a "Controller support: install ConsolePortLK" card that pulls the latest GitHub release of `leoaviana/ConsolePortLK` into `Interface/AddOns` and tracks updates like any other addon. Downloaded at install time, not bundled (Artistic License 2.0).
2. **Controller-to-keyboard profile.** 3.3.5a can't read a gamepad, so ship a "ConsolePortLK" input profile (buttons and triggers to keys, right stick to mouse) and use it automatically for that client. Without it the addon does nothing.

## Proposed by Claude, not agreed yet

- **Keyboard on the 3.3.5a login screen.** Addons can't run on the login, code and character screens, so typing there means Back, then Keyboard. Either open the keyboard by itself when the client starts, or map a controller button to it for the private server container.

- **Own the CurseForge proxy.** Release builds without a key send every CurseForge request to `cf.yorudev.com`, upstream's server. Either run `server/curseforge-proxy` with Daniel's own key and build with `-PCURSEFORGE_PROXY=...`, or accept upstream's server and say so in NOTES.md.
