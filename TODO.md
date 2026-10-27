# TODO

Only work that's left. Shipped items move to DONE.md, rejected ones to DROPPED.md.

## Fork housekeeping

1. **Addon catalog link.** The GitHub addon catalog (`GitHubProvider.CATALOG_URL`) still points at upstream. It can stay on upstream's nightly build (no cost, but it's their branch) or the fork's `addon-catalog` workflow can publish its own.

## 3.3.5a speed on the Odin 3

Measured on the Odin 3 (2026-10-24): DXVK, 30-50 fps, GPU load 26-32%, so the processor (Box64) is the limit. Lighter graphics shipped (see DONE.md). If that's not enough:

1. **Faster Box64 option.** An opt-in "Faster emulation" choice adding `BOX64_DYNAREC_SAFEFLAGS=0`, `BOX64_DYNAREC_FORWARD=1024` and `BOX64_DYNAREC_DIRTY=1` on top of the Performance preset. These can crash some code paths, so off by default and measured one at a time with the overlay.
2. **Newer wowbox64.** The bundled one is 0.4.2; a newer build may run x87 and SSE code faster. Needs a build from Box64's releases packaged as `wowbox64-<ver>.tzst`.

## Client add-ons (3.3.5a)

1. **Controller without ConsolePortLK.** The pad only sends keys while ConsolePortLK's profile is in use. A small profile for players without it (D-pad as arrows, A as Enter, B as Escape) would let the login screens work on their own.
2. **The HD pack's rules.** The WotLK HD pack's menu keeps some patches together (u needs a, n needs m) and apart (j and k). The Client tab doesn't yet; its config file has the rules to read.

## Later

- **In-game extras from the HD pack as addons.** Its patch 5 and 9 carry later-expansion raid frames, loss of control alerts and a level up banner. They need no client patch, so they'd fit the 3.3.5a addon catalog as backports, if good 3.3.5a versions exist. Daniel: keep for later. (2026-10-27)

## Proposed by Claude, not agreed yet

- **Keyboard on the 3.3.5a login screen.** Addons can't run on the login, code and character screens, so typing there means Back, then Keyboard. Either open the keyboard by itself when the client starts, or map a controller button to it for the private server container.
