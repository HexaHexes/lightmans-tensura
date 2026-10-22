# Agent rules and conventions — Lightman's Tensura Economy

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

**Gotchas**
- The `tensura:coins` tag is overridden with `replace: true`; pouch recipes are disabled
  via `neoforge:false` conditions.
- `/coinconvert` runs at permission level 2 and converts stacks 1:1 by tier.
- Nine wallet tiers are registered to Gear Evolution via
  `data/lightmans_tensura/addon_chains/` (minEP ladder 0 → 80000).
- The migration converts currency on chunk/entity load, player login, and via
  `/coinconvert` — keep all four surfaces in sync when touching conversion.

---

## Branch sync (do this first)

Before starting any new development in this repo:

1. `git status` — confirm what uncommitted work exists. Never disturb or sweep it.
2. `git pull --ff-only` — sync with the remote. A fast-forward is the normal case,
   because we push straight to the shared branch.
3. If the pull would **not** fast-forward (the branch has diverged), or if it would
   collide with local uncommitted work, **stop and ask** rather than merging, rebasing
   or stashing on your own.

## Scope doctrine

- **One mod + bundled datapack.** No KubeJS scripts.
- **Data-driven over code** — prefer JSON/datapack content to Java when both work.
- **Public API only, no mixins** in the colony-gear-style addon mods. Sanctioned
  exception: `ds-tensura-fix` (tensura-dragon-skin-rebalance), a mixin fix mod by design.

## Verification

- Verify with the in-repo GameTest run (`./gradlew runGametest`, or `runGameTestServer`).
  Repos without GameTests verify with `./gradlew build`. Never verify against a live
  CurseForge instance.
- Keep `expectedTests` in `build.gradle` in sync with the `@GameTest` methods.
- **Client-render features cannot be proven headless.** The honest scope is
  "build green + the user's in-game check" - say so rather than overclaiming.


## Build environment

- JDK 21 (`JAVA_HOME` -> JDK 21); the toolchain targets Java 21.
- The Gradle wrapper is committed. `./gradlew build` compiles, `./gradlew runGametest`
  (or `runGameTestServer`) runs the GameTest server.
- **Dependencies resolve through Maven, never local folders.** Compile-only mods use
  CurseMaven coordinates (`curse.maven:<slug>-<projectId>:<fileId>`, repository
  `https://cursemaven.com`). Runtime mod closures for GameTest runs live in the
  `gametestMods` Gradle configuration, synced into `run/mods` by `prepareGametestMods`.
  Gitignored `libs/` and `testmods/` staging folders are retired: CI must resolve
  every dependency from a clean checkout. Pin the exact version the repo was
  verified against; bump pins deliberately, never implicitly. The manascore core jar
  nests its `manascore-*` modules under `META-INF/jars/` - stage only the core.
- On `run/` file-lock errors, kill any stale `java` process and retry.

## PowerShell quirks (this org is developed on Windows)

- A `git push` "error" in PowerShell is usually stderr passthrough — success is the
  `hash..hash  branch -> branch` line, not the absence of red text.
- `New-Object Type(args)` breaks on arithmetic — use `::new()` or a parenthesized
  argument.

## Git & release

- Remote is SSH: `git@github.com:HexaHexes/<name>.git`.
- Push only after tests are green **and** the user has given the go.
- Never sweep the user's uncommitted work into a commit — commit only what is intended.
- Commit subjects: concise, imperative.
- **Version bump on finish:** always bump the version when a new feature or bugfix
  is finished.
- **One feature or bugfix per branch:** develop each on its own feature branch and
  merge it into main/master through a pull request with a **squash merge**. The PR
  body must detail the work done, following `.github/pull_request_template.md`.

## House style

- **No icons** unless the user explicitly asks. Icons are being removed across all repos.
- **No emojis, ever.** Prohibited in all code, docs, configs, and commit messages.
- READMEs are **player-facing only**: pitch, mechanics in plain terms, in-game usage,
  required/optional dependencies, credits, license. No build internals, no class names.
- All repos are **LGPL v2.1**, copyright Bitwise Madness. `NOTICE` / attribution files
  are mandatory and must never be deleted.
- Deploy to the Tensura RPG instance **only** when the user asks.

## Naming

- **Java base package:** `org.bitwisemadness.<project-name>` - repo name lowercased,
  separators stripped (e.g. `ds-tensura-fix` -> `org.bitwisemadness.dstensurafix`).
  The Maven `group` in `build.gradle` matches (`org.bitwisemadness`).
- Keep repo name, folder name, `mod_id`, and the Java package aligned; the rename
  checklist in the template's `README.md` walks the whole set.
- **The template (`neoforge-mod-template`) is canonical for org-wide rules.** When a
  rule is added or changed in any repo's `agents/AGENTS.md`, mirror the change into
  the template so new repos inherit it.

## CI & release model

- `ci.yml` runs three checks that back the branch protection rulesets: **junit**
  (build + the JUnit suite, asserted non-vacuous), **gametest** (`runGametest`,
  graded on the `All N required tests passed` log line - the exit code is not a
  reliable signal) and **version-bump** (a beta or stable bump must change the
  version file and nothing else; an `-alpha` bump may bundle work; a PR labelled
  `breaking` forces the next clean bump to raise the major or minor version).
- `release.yml` is **not yet enabled for the gradle mod repos** - it needs a CI-built jar
  to attach. The datapack repo is the exception: its "build" is a zip, so it has a working
  `release.yml` that tags and publishes on a version bump.
- `version-bump.yml` opens an isolated version-bump PR (only the version file changes).
- CurseForge publishing (`curseforge.yml`) is **deferred** until a mod has a CurseForge
  listing and the `CURSEFORGE_PROJECT_ID` / `CURSEFORGE_API_KEY` secrets. Do not add it
  speculatively.
- Where a release workflow exists, alpha/nightly publishing is opt-in via the
  `ALPHA_RELEASES` repo variable and is **off** by default.

