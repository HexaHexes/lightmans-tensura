# Lightman's Tensura Economy

Swaps Tensura's coins and pouches for **Lightman's Currency** coins and wallets, so the
whole economy runs on one currency system. If a world already has Tensura coins or
pouches in it, they convert to the new currency automatically.

## What it does

- Nothing to set up. Coins and wallets drop, trade and craft wherever Tensura's
  currency already did.
- One currency. Tensura's coins become Lightman's Currency coins, and Tensura's
  pouches become Lightman's wallets. The five tiers map one-for-one:
  d → leather, c → copper, b → iron, a → gold, special_a → netherite.
- Existing money carries over. Coins convert as you play: when chunks and
  entities load, when you log in (inventory, armor, offhand, ender chest), and on
  demand with `/coinconvert` (operator, permission level 2). Pouch contents move
  straight into the matching wallet.
- Villager trades pay the new currency. Villager offers are converted too, so
  trading works exactly as before, with the new coins.
- With **Gear Evolution** installed, wallets slot into its upgrade ladder, so a
  better wallet is a real tier you work up to (leather through netherite, ending
  at the ender-dragon wallet). Without it, wallets still work; they just don't
  evolve.
- Quest and market rewards pay Lightman's coins when Tensura: All The Adventure
  is installed.

## Dependencies

| | Mod | Why |
|---|---|---|
| **Required** | [Lightman's Currency](https://www.curseforge.com/minecraft/mc-mods/lightmans-currency) | The coins and wallets |
| Optional | [Gear Evolution](https://www.curseforge.com/minecraft/mc-mods/gear-evolution) | Wallet upgrade ladder |
| **Required** | Tensura: Reincarnated | The currency being replaced |
| **Required** | ManasCore | Tensura's core library |
| Optional | Tensura: All The Adventure | Quest/contract/market rewards pay Lightman's coins |

Made for the **Tensura RPG** pack (Minecraft 1.21.1, NeoForge).

## Credits

- Built on [Lightman's Currency](https://www.curseforge.com/minecraft/mc-mods/lightmans-currency) and [Gear Evolution](https://www.curseforge.com/minecraft/mc-mods/gear-evolution).

## License

GNU Lesser General Public License v2.1 — see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.

## AI disclosure

Parts of this project were written with the assistance of AI coding tools.
AI-assisted code, data and documentation ship only after human review by the
maintainers.
