# Vortex Client – Architecture & Roadmap

Two deliverables, one repo:

```
vortex/
  mod/        Fabric client mod (Minecraft 1.21.1, Java 21, Yarn mappings)
  launcher/   Standalone JavaFX launcher (Microsoft login, mod browser, downloads)
  docs/
```

## 1. Mod modules

| Package | Responsibility |
|---|---|
| `dev.vortex` | Entrypoint (`VortexClient`), keybind (RIGHT SHIFT), tick/autosave/shutdown hooks |
| `dev.vortex.module` | `Module`, `Setting`, `Category`, `ModuleManager` (single registry; hot-path settings are static fields so mixins never do map lookups or allocate) |
| `dev.vortex.config` | `ConfigManager`: JSON in `config/vortex/config.json`, atomic write (tmp + move), corrupt file quarantined, per-value validation, debounced autosave |
| `dev.vortex.mixin` | Rendering/engine hooks. Every mixin checks its module's `enabled` flag first, so disabled = vanilla behaviour |
| `dev.vortex.ui` | `Theme`, `VortexButton`, `VortexTitleScreen`, `ModMenuScreen`, `ModSettingsScreen` |
| `dev.vortex.hud` | `HudRenderer` (cached strings, no per-frame allocation when values are unchanged) |

Planned (later stages): `profile` (save/export/import), `perf.dashboard` (FPS graph, 1% lows, benchmark), `hud.editor` (drag/resize), `perf.preset` (LOW/BALANCED/HIGH + OPTIMIZE MY GAME).

## 2. APIs used
- Fabric Loader, Fabric API: `ClientModInitializer`, `ClientTickEvents`, `ClientLifecycleEvents`, `HudRenderCallback`, `KeyBindingHelper`
- Minecraft (Yarn): `MinecraftClient`, `Screen`, `DrawContext`, `ParticleManager`, `EntityRenderDispatcher`
- Mixin (bundled with Fabric Loader)

## 3. Compatibility limits (honest list)
- **Sodium/Embeddium-style chunk work is not reimplemented.** Rewriting the chunk renderer collides with Sodium and is a multi-year project. Plan: Vortex does *not* touch chunk meshing; the launcher ships Sodium, Lithium, FerriteCore, EntityCulling, ImmediatelyFast, ModernFix as a one-click "Optimizer Pack", and Vortex adds the UI + monitoring on top.
- Title-screen replacement only targets the exact vanilla `TitleScreen` class, so other mods that subclass it are left alone. Toggle in config (`customTitleScreen`).
- Mixins use Yarn names for 1.21.1. Other MC versions need a new `gradle.properties` and possibly mixin target fixes.
- HUD uses `HudRenderCallback` (deprecated in later Fabric API, still works on 1.21.1).

## 4. Fairness policy
- Entity culling **never hides players** and never hides very large entities (bosses). Min distance is 32 blocks. Culling is off by default and clearly labelled.
- Particle filtering only skips *cosmetic* explosion/weather particles or ones beyond a user-set distance.
- No networking, packet, or server-side behaviour is touched.

## 5. Launcher
1. **Microsoft sign-in**: OAuth device-code flow → Xbox Live → XSTS → Minecraft Services → profile. Refresh token stored locally.
2. **Mod browser**: Modrinth API v2 (search, version lookup, required-dependency resolution), SHA-512 verified downloads, host allow-list (`*.modrinth.com`).
3. **Optimizer Pack**: curated slug list, one click.
4. Next stage: Mojang version manifest → libraries/assets download → Fabric profile install → process launch with the signed-in session.

**Hard requirement for login:** you must register your own Azure app (public client, "personal Microsoft accounts", device-code flow allowed) and set `VORTEX_MS_CLIENT_ID`. New apps also need approval from Mojang before Minecraft Services accepts them (https://aka.ms/AppRegInfo); until approved, login fails at the Minecraft step with an "invalid app registration" error. This is a Mojang gate, not something code can bypass.

## 6. Roadmap
1. ✅ Core: config, modules, Right-Shift menu, title screen, FPS limiter, particle optimizer, entity culling, 2 HUD elements
2. HUD editor, remaining HUD elements (CPS, ping, coords, keystrokes, armor, potions, clock, CPU)
3. Animation toggles (water/lava/fire/portal) – needs verified mixin targets on a real dev env
4. Profiles + export/import; presets + OPTIMIZE MY GAME
5. Performance dashboard + benchmark mode (repeatable flythrough, before/after report) – **all FPS claims come from this, not guesses**
6. Launcher: game install + launch, instance manager, bundling the Vortex jar
7. Themes, cosmetics placeholder → real system
