# EgyptianCasino Plugin

EgyptianCasino is a Paper plugin that extends the CrystalMath (or legacy AmethystControl) economy with Egyptian Tokens, immersive NPC exchanges, and modular casino experiences. Players can convert minted crystals from AmethystControl into tokens, then wager them on minigames such as blackjack, dice, and the new Pharaoh Slots machine.

## Requirements

- Paper (or another Spigot-compatible server) 1.19+
- Either the [CrystalMath](https://github.com/) plugin (v1.0.0 or later) or the legacy [AmethystControl](https://github.com/) plugin installed and configured with minted crystals
- Java 17+

## Installation

1. Build the plugin with Gradle:
   ```bash
   ./gradlew build
   ```
   The wrapper script downloads and caches the Gradle distribution on first run (requires `curl` or `wget`). The compiled plugin will be placed in `build/libs/EgyptianCasino-<version>.jar`.
2. Copy both `EgyptianCasino-<version>.jar` and your existing `CrystalMath` (or `AmethystControl`) plugin into the server's `plugins/` folder.
3. Restart or reload the server.

Player balances are written to `tokens.yml` in the plugin data folder.

## Configuration

The default configuration is stored in `config.yml` and copied to the plugin's data folder on first run. You can edit the file and reload the plugin to change its behaviour:

```yaml
# Number of Egyptian Tokens granted per minted crystal redeemed with the Priest of Ra
tokens-per-crystal: 1

priest:
  # When true, spawned priests will be frozen in place and cannot walk away.
  disable-movement: true
  # Make the Priest of Ra invulnerable to damage.
  invulnerable: true
```

- `tokens-per-crystal`: sets how many Egyptian Tokens a single minted crystal is worth when exchanged.
- `priest.disable-movement`: toggles whether the Priest of Ra can wander away after being spawned.
- `priest.invulnerable`: protects the Priest of Ra from all damage.

After making changes, restart the server (or reload the plugin) to apply them.

## Commands

| Command | Description |
| --- | --- |
| `/egyptiancasino` | Shows plugin help, including registered minigames. |
| `/egyptiancasino balance` | Displays the player's current Egyptian Token balance. |
| `/egyptiancasino <game> <bet>` | Plays a registered casino game (slots, blackjack, dice). |
| `/egyptiancasino spawnpriest` | Spawns the Priest of Ra villager (admin only). |
| `/egyptiancasino givetokens [amount]` | Gives gold nugget token items with `CustomModelData:1010` for resource-pack testing (admin only). |
| `/giveslot [amount]` | Gives the Pharaoh Slot Machine placement item (admin only). |

> **Tip:** Whenever a player checks their balance or places a bet, the plugin automatically moves any Egyptian Token items (gold nuggets with `CustomModelData:1010`) from their inventory into the persistent balance ledger.

## Spawning the Priest of Ra

Administrators can summon the exchange NPC directly in game:

1. Ensure your player has the `egyptiancasino.admin` permission.
2. Stand at the location where you want the NPC to appear.
3. Run `/egyptiancasino spawnpriest`.

This command spawns a desert villager named **Priest of Ra**. When players right-click the Priest, any minted crystals in their inventory are converted into Egyptian Tokens automatically. Each crystal is validated and marked as redeemed through AmethystControl family, so they cannot be exchanged twice.

If you ever move or respawn the Priest manually, you can reapply the identification tag by running `/egyptiancasino spawnpriest` again.

## Pharaoh Slots Machine

Pharaoh Slots introduces a physical, two-block tall slot machine that uses BlockDisplay and ItemDisplay entities to render an ancient Egyptian cabinet with turquoise inlays and glowing crystals.

### Placement and Ownership

1. Use `/giveslot` to obtain the **Slot Machine** item (base material: Blaze Rod with `CustomModelData:2100`).
2. Right-click any solid surface with at least two blocks of vertical clearance to place the machine.
3. The placer becomes the owner. Only the owner can dismantle it by sneaking and left-clicking any part of the machine.

The item uses the plugin's `PersistentDataContainer` markers to track ownership. When a machine is broken by its owner, the placement item is dropped back at the base location.

### Gameplay Flow

- Right-click the machine's base block or lever (any player) to pull the handle.
- The machine withdraws **1 Egyptian Token** from the player's balance.
- A lever animation plays, the reels spin for ~3 seconds, and stop one-by-one.
- Outcomes are processed through `SlotMachineManager`/`EgyptSlots`:
  - **3 matching symbols:** rewards +5 tokens, plays a victory fanfare, and emits golden glow particles.
  - **2 matching symbols:** rewards +1 token with a small win sound and sparks.
  - **0 matches:** no reward, with a sandy puff effect.

Results, particles, and sounds are broadcast to nearby players, ensuring the experience feels shared on busy servers.

### Editing Reel Symbols or Rewards

The reel `CustomModelData` values and payout table are centralized in [`src/main/java/dev/lixqa/egyptiancasino/slotmachine/EgyptSlots.java`](src/main/java/dev/lixqa/egyptiancasino/slotmachine/EgyptSlots.java). Update the list in `createDefaultSymbolList()` to align with your resource-pack models, and adjust the `rewardForMatches()` switch statement to rebalance winnings.

## Resource Pack Setup

The `resource-pack/` directory contains JSON stubs for the Pharaoh Slots assets. Replace the placeholder textures, sounds, and animation references with your production files.

### 1. Custom Items

- `assets/minecraft/models/item/gold_nugget.json` – Adds an override for `CustomModelData:1010` → `item/egypt_token`. Supply **`textures/item/egypt_token.png`** as a PNG file (32×32 minimum, 64×64 or 128×128 recommended for crisp gold filigree details).
- `Slot Machine` placement item uses Blaze Rod with `CustomModelData:2100`. Provide an inventory icon model (for example `assets/minecraft/models/item/slot_machine.json`) that references a **PNG texture** such as `textures/item/slot_machine.png` (64×64 or 128×128 recommended).
- `assets/minecraft/models/item/paper.json` – Supplies overrides for the reel symbols (`CustomModelData:3001-3003`) and the slot cabinet body (`CustomModelData:3200`). Point those overrides to your Blockbench exports under `models/item/slot_reel_*.json` and `models/item/slot_machine_body.json`, each of which can simply inherit from the corresponding block model.

### 2. Block Models & Animations

Export your Blockbench models as `.json` and drop them into the paths below. Each model should reference **PNG block textures** (at least 64×64, 128×128 recommended for close-up clarity):

```
resource-pack/
└── assets/minecraft/
    ├── models/
    │   ├── block/egyptslot_machine.json
    │   ├── block/slot_reel_1.json
    │   ├── block/slot_reel_2.json
    │   ├── block/slot_reel_3.json
    │   ├── block/slot_lever.json
    │   └── item/slot_machine_body.json
    ├── animation_controllers/
    │   ├── slot_lever.controller.json
    │   └── slot_reel.controller.json
    └── animations/
        ├── slot_lever.animation.json
        └── slot_reel_spin.animation.json
```

The provided stubs include `_comment` fields marking where to hook up your final texture names (for example `block/egyptslot_machine.png`, `block/slot_lever.png`, a turquoise inlay map, and an emissive glow layer). Keep each PNG texture square (64×64 minimum, 128×128 preferred) so UVs remain sharp on the display entities. The plugin renders the cabinet via ItemDisplay entities that consume the `slot_machine_body` custom item model, so normal Smooth Sandstone blocks in your world keep their vanilla appearance.

### 3. Animation Controllers

- `animation_controllers/slot_lever.controller.json` – Watches the `variable.slot_machine_lever_pull` Molang value. When the value rises above `0.5`, the controller plays `animation.slot_lever.pull` once and returns to idle as soon as `query.all_animations_finished` reports `true`.
- `animation_controllers/slot_reel.controller.json` – Listens for `variable.slot_machine_reel_spin > 0.5` to fire up `animation.slot_reel.spin`, then falls back to `idle` when the animation completes.

Attach the controllers to your display entities (or whichever animation system you use) and toggle the referenced variables via commands or datapacks when the plugin announces lever pulls or reel spins. Resetting the values to `0` arms the controllers for the next cycle.

### 4. Hooking Up Animations

- `animations/slot_lever.animation.json` – Contains forward/back keyframes for a 0.6 second lever pull (32° swing). Rename the `lever` bone and adjust keyframes to match your Blockbench rig. Save as a Blockbench/Bedrock **animation JSON** file targeting the lever bone hierarchy.
- `animations/slot_reel_spin.animation.json` – Spins a reel bone twice over three seconds. Tie this to your reel bone or use texture cycling if you prefer frame-based symbols. This JSON expects a Blockbench animation file that rotates a bone named `reel`. Duplicate and rename per reel if you separate symbols, and point any additional animation controllers at the new names.

### 5. Asset Requirements Checklist

| Asset | File type | Recommended resolution / format | Purpose |
| --- | --- | --- | --- |
| `textures/item/egypt_token.png` | PNG (32×32 minimum, 64×64+ recommended) | Square texture with alpha, metallic gold + turquoise inlay | Inventory icon for Egyptian Tokens |
| `textures/item/slot_machine.png` | PNG (64×64 or 128×128) | Square texture with alpha | Optional inventory icon for Slot Machine item |
| `textures/block/egyptslot_machine.png` | PNG (64×64 minimum, 128×128 preferred) | Square texture; can include emissive companion maps if your pack supports them | Main cabinet body |
| `textures/block/slot_reel_1.png`, `_2.png`, `_3.png` | PNG (64×64 minimum) | Individual reel symbol strips or baked textures | Reel surfaces |
| `textures/block/slot_lever.png` | PNG (64×64 minimum) | Lever arm texture | Lever component |
| Optional emissive/glow textures | PNG (match base texture resolution) | Use `_e` suffix if your pack uses OptiFine/CustomModelData emissives | Crystal glow accents |
| `animations/slot_lever.animation.json` | Blockbench animation JSON | Uses `animation_length: 0.6`, lever bone rotation keys | Lever pull animation |
| `animations/slot_reel_spin.animation.json` | Blockbench animation JSON | Uses 3s rotation timeline, applies to `reel` bone | Reel spinning animation |
| Block models listed above | Blockbench model JSON | Ensure pivot points align with plugin positions; origin at 0,0,0 | Slot machine structure |

All textures should be **PNG files with transparency** where appropriate. If you plan to supply normal or specular maps, follow your pack's pipeline (e.g., `*_n.png` / `*_s.png`). Place audio cues (lever pull, win/loss sounds) as `.ogg` files under `assets/minecraft/sounds/` and update `sounds.json` if you wish to customize the default Minecraft sounds triggered in `EgyptSlots`.

### 6. Registering CustomModelData

Ensure your item overrides point to the models above. The gold nugget override is preconfigured for the Egyptian Token. Update `resource-pack/assets/minecraft/models/item/gold_nugget.json` if you change model names or add additional predicates, and double-check that each referenced texture path exists in `textures/` with the file names listed in the checklist.

### 7. Testing In-Game

1. Load the resource pack on your test client or server.
2. Run `/egyptiancasino givetokens 16` to preview the Egyptian Token item.
3. Run `/giveslot` to obtain the Slot Machine item.
4. Place the machine, pull the lever, and confirm the lever/reel animations line up with your models.

> **Note:** The packaged textures and animation JSONs are placeholders. The server operator must supply the final `.png` files and fully animated `.animation.json` content before deployment.

## Token Storage

Player balances are tracked in memory and saved asynchronously to `tokens.yml`. The token manager guards against concurrent updates so balances remain consistent even when multiple games operate at once.

## Development Notes

- Game logic lives in `src/main/java/dev/lixqa/egyptiancasino/game/`.
- Pharaoh Slots runtime code resides in `src/main/java/dev/lixqa/egyptiancasino/slotmachine/`.
- Token persistence is handled through `TokenManager` and `YamlTokenStorage` in `src/main/java/dev/lixqa/egyptiancasino/tokens/`.
- The Priest of Ra functionality is encapsulated in `PriestOfRaManager` and the `CrystalExchangeListener` under `src/main/java/dev/lixqa/egyptiancasino/exchange/`.

Feel free to add new games by implementing the `CasinoGame` interface and registering the instance in `EgyptianCasinoPlugin#setupGames`.
