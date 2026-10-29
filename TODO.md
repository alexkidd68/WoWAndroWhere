# TODO

Only work that's left. Shipped items move to DONE.md, rejected ones to DROPPED.md.

## Next: older private server versions, the rest

1.12.1, 2.4.3, 4.3.4 and 5.4.8 basic play is built (see DONE.md). What they still lack:

- **Controller Menus on 1.12.1 and 2.4.3.** Find which bytes of the 5875 and 8606 exes stop the "login interface files are corrupt" check, on Daniel's PC like the 3.3.5a ones, then port the login, realm and character Lua to each client's GlueXML. Add each to `LegacyGame.hasAndroWherePatches` and give `WowExePatch` a spot list per build.
- **Grow the 1.12.1 and 2.4.3 addon catalogs.** Read the first workflow run's log to see which starter repos were kept, then add more from the community lists, the way `wotlk335.json` was filled.
- **Addon catalogs for 4.3.4 and 5.4.8.** Same as above, once Daniel says either runs well enough to play.
- **Find the fastest settings for 4.3.4 and 5.4.8.** Measure fps against 3.3.5a on the same spot, then decide whether their own lighter-graphics values or Box64 flags are worth it.
