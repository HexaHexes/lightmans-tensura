# Repo rules - Lightman's Tensura Economy

Repo-specific rules and notes. The org-wide `AGENTS.md` beside this file is
maintained in `neoforge-mod-template` and pulled in verbatim - **never hand-edit it.**
Everything in this file is this repo's own and is edited freely; keep it aligned
with the org rules rather than restating them.

**Repo:** `HexaHexes/lightmans-tensura` · **Mod ID:** `lightmans_tensura` · **Branch:** `master`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 · **Jar:** `build/libs/lightmans-tensura-<version>.jar`

Replaces Tensura's coins/pouches with Lightman's Currency coins/wallets as one mod +
a bundled datapack. Wallets progress through Gear Evolution's `addon_chains/`.

**Dependencies**
- Required: Lightman's Currency (`lightmanscurrency`), Tensura (`tensura`),
  ManasCore (`manascore`).
- Optional: Gear Evolution (`gearevolution`) — registers the wallet upgrade ladder via
  `addon_chains/`; without it wallets still work but do not evolve. Tensura: All The
  Adventure (`tensura_all_the_adventure`) — enables its quest/contract/market rewards
  paying Lightman's coins (gated mixins).

**Test:** `./gradlew runGametest` — `EconomyGameTests` (14 tests). `expectedTests = 14`.

**Remote:** `github.com/HexaHexes/lightmans-tensura` (public, default branch
`master`).

---

## Gotchas (do not regress them)

- The `tensura:coins` tag is overridden with `replace: true`; pouch recipes are disabled
  via `neoforge:false` conditions.
- `/coinconvert` runs at permission level 2 and converts stacks 1:1 by tier.
- Nine wallet tiers are registered to Gear Evolution via
  `data/lightmans_tensura/addon_chains/` at Gear Evolution's canonical material
  progression values: leather 0 → 550, copper (chainmail's slot) → 1350, iron → 3200,
  gold → 5000, emerald (wallet-only rung) → 6400, diamond → 7800, netherite → 17500,
  nether_star (magisteel tail) → 18000, ender_dragon → 52000 terminal. Climb rates
  are flat 0.003 through netherite (what canonical chestplates use), then 0.01/0.02
  on the tail. `tensura-tool-belt` mirrors the same ceilings minus emerald.
- The migration converts currency on chunk/entity load, player login, and via
  `/coinconvert` — keep all four surfaces in sync when touching conversion.

## Scope doctrine - repo additions

- **Gated mixins are sanctioned here** for the Tensura: All The Adventure integration
  (reward redirection only). The org rule "public API only, no mixins" still applies
  to new addon repos.

## Build environment - repo additions

- Runtime mod closures for GameTest runs live in the `gametestMods` Gradle
  configuration, synced into `run/mods` by `prepareGametestMods`.
- The manascore core jar nests its `manascore-*` modules under `META-INF/jars/` -
  stage only the core.

## Naming - this repo

- Repo and folder: `lightmans-tensura`; `mod_id`: `lightmans_tensura`; Java base
  package `org.bitwisemadness.lightmanstensura`, with the Maven `group` in
  `build.gradle` matching. The jar name keeps the hyphen (`lightmans-tensura`).

## CI & release model - this repo

- `ci.yml` runs the template's three checks, with the gametest job grading
  `./gradlew runGametest`.
- `release.yml` cuts the GitHub release for `v<mod_version>` on a push to
  `master` (this repo''s default branch), and `publish.yml` publishes the Maven
  artifact to the org''s GitHub Packages registry when that release is
  published - pre-releases only when the repo variable `ALPHA_RELEASES` is true.
- The release workflow''s jar path uses this repo''s hyphenated `archivesName`
  (`lightmans-tensura`), not the `mod_id` the template derives it from - keep
  that local edit if the file is ever re-copied from the template.
