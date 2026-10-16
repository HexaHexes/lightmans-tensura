# Lightman's Tensura Economy

NeoForge 1.21.1 mod (`lightmans_tensura`) for the Tensura RPG pack. Replaces Tensura's
coins/pouches with Lightman's Currency coins/wallets — **one mod + bundled datapack**, no
KubeJS scripts. All changes live in Java or `src/main/resources/data/`.

## What it does

- **Retires Tensura's currency items.** The `tensura:coins` tag is overridden
  (`replace: true`) to Lightman's Currency main-chain coins; pouch recipes and their
  recipe advancements are disabled (`neoforge:false` conditions); pouches become LC
  wallets 1:1 by tier (pouch d/c/b/a/special_a → wallet leather/copper/iron/gold/netherite).
  Old coin/pouch items stay registered but are converted out of existence everywhere.
- **Migrates existing currency** in the world: converted when chunks/entities load
  (`EntityJoinLevelEvent`, `ChunkEvent.Load`), when players log in (inventory, armor,
  offhand, ender chest), and on demand with `/coinconvert` (permission level 2).
  Coin stacks convert 1:1 by tier, splitting across stacks when needed; pouch contents
  transfer into the replacement wallet.
- **Converts villager trade offers** via a single `MerchantOffer.<init>` mixin
  (three `@ModifyVariable` handlers): buy cost, second cost and result stacks convert,
  vanilla items pass through untouched, and `copy()` keeps conversions.
- **Wallet progression through Gear Evolution**: all nine wallet tiers are registered
  with Gear Evolution via `data/lightmans_tensura/addon_chains/` (its extension point
  for non-builtin items) with a minEP ladder of 0/1000/2000/4000/8000/16000/32000/50000/80000
  and `wallet_ender_dragon` as the terminal tier.
- **Bundled datapack overrides**: loot tables and advancements, the
  `minecraft:indestructible_by_environmental_cause` tag (wallets included), and lang
  overrides for Tensura and — conditionally, when `tensura_all_the_adventure` is
  present — its quest/contract/market rewards paying Lightman's coins (gated mixins).

## Building

```
gradle build        # → build/libs/lightmans-tensura-1.0.0.jar
```

Compile-only mod jars go in `libs/` (gitignored).

## Gametests (in-repo, never a CurseForge instance)

```
gradle runGametest
```

Boots a headless `GameTestServer` inside this repo's `run/` folder
(`-Dneoforge.gameTestServer=true`, namespace-locked to `lightmans_tensura`), staging the
pack's dependency jars from `testmods/` (gitignored) into `run/mods/` for
production-parity mod discovery. The run grades `run/logs/latest.log` and fails the Gradle
build unless all tests report pass.

`EconomyGameTests` (14 tests) covers datapack override order (coins tag, disabled
recipes/advancements, advancement overrides, env tag), the trade mixin, all migration
surfaces (entity join, containers, player inventory + ender chest), `/coinconvert`, and
the wallet Gear Evolution chain.
