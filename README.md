# Vortex Client (beta)

```
mod/             Fabric mod (MC 1.21.1, Java 21)      -> cd mod && gradle build  -> build/libs/vortex-client-0.1.0.jar
launcher/     JavaFX launcher (Java 21)            -> cd launcher && gradle run
docs/         ARCHITECTURE.md (modules, limits, fairness rules, roadmap)
```

## Status: NOT yet compiled or tested
Written in a sandbox with no access to Fabric's Maven, the Gradle plugin portal or Modrinth, so
**nothing has been built or run yet**. Expect a few compile errors on the first build (most likely
Yarn method names in the mixins, or version numbers in `mod/gradle.properties`). Send me the
error output and I'll fix it. No FPS numbers are claimed anywhere: the benchmark mode that would
produce them is not built yet.

## Mod: what works (in code)
- Right Shift opens the Vortex menu: sidebar, search, favorites, cards with toggles, per-mod settings screens, reset.
- Appearance page: 4 themes, 6 backgrounds (Aurora, Nebula, Grid, Spotlight, Solid, **Custom PNG path**), animation speed, title-screen on/off.
- Custom title screen with an original animated logo and rounded hover buttons (Cosmetics is a disabled placeholder).
- Real optimizations: FPS limiter (unfocused + menu), particle optimizer (distance/explosion/weather), entity distance cull (never players/bosses/large mobs, min 32 blocks, off by default).
- HUD: FPS + frame time, memory, coordinates (HUD text is cached, rebuilt every 250 ms).
- Zoom (hold C).
- Config: atomic saves, corrupt-file quarantine, per-value validation, debounced autosave.

## Mod: not built yet
HUD editor (drag/resize; positions are editable via sliders), CPS/ping/keystrokes/armor/potions/clock/CPU HUDs,
animation toggles, profiles + import/export, presets + OPTIMIZE MY GAME, dashboard/FPS graph/benchmark.
Chunk work is intentionally left to Sodium (see ARCHITECTURE.md).

## Launcher: what works (in code)
- Microsoft login via device code (Xbox Live -> XSTS -> Minecraft), silent re-login, sign out.
- Modrinth search, install with required dependencies, SHA-512 verified, cdn.modrinth.com only.
- "Install Optimizer Pack" (Sodium, Lithium, FerriteCore, EntityCulling, ImmediatelyFast, ModernFix, Krypton, Fabric API; slugs unverified here).
- Configurable theme/background (incl. your own image), animated logo.

### You must do for login to work
1. Register an app at portal.azure.com (App registrations): account type "personal Microsoft accounts", enable **Allow public client flows**.
2. Paste its client ID into Appearance > Microsoft client ID (or set `VORTEX_MS_CLIENT_ID`).
3. New apps also need Mojang approval (https://aka.ms/AppRegInfo) before the Minecraft step succeeds.

### Launcher: not built yet
Downloading Minecraft + Fabric and **launching the game** (the Play button). For now, mods download into
`~/.vortex/instances/<version>/mods`; copy them to your normal Fabric `.minecraft/mods` or point another launcher at that folder.
Local refresh token is stored in `~/.vortex/account.json` (owner-only permissions on Linux/macOS; not encrypted).
