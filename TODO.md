# TODO

Only work that's left. Shipped items move to DONE.md, rejected ones to DROPPED.md.

## Next: older private server versions, the rest

1.12.1 and 2.4.3 basic play is built (see DONE.md). What they still lack:

- **Controller Menus on 1.12.1 and 2.4.3.** Find which bytes of the 5875 and 8606 exes stop the "login interface files are corrupt" check, on Daniel's PC like the 3.3.5a ones, then port the login, realm and character Lua to each client's GlueXML. Add each to `LegacyGame.hasAndroWherePatches` and give `WowExePatch` a spot list per build.
- **Grow the 1.12.1 and 2.4.3 addon catalogs.** Read the first workflow run's log to see which starter repos were kept, then add more from the community lists, the way `wotlk335.json` was filled.
- **Cataclysm 4.3.4 private servers.** Still 32-bit with realmlist, so the same path, but a much heavier client on an already CPU-bound setup; expect lower fps than 3.3.5a (a guess). Try after 1.12.1 and 2.4.3 run on the Odin.

## Parked

- **Mists of Pandaria 5.4.8 private servers: parked.** The 32-bit client is the heaviest under Box64 and the 64-bit one needs FEX x64, which isn't bundled. Official MoP Classic already runs natively. Revisit after Cataclysm shows how far emulation goes.
