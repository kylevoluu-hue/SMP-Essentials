# SMP Essentials — Resource Pack

Gives the ability items custom models via `custom_model_data` (the new 1.21.4+
item-model definition format used by Minecraft 26.1).

## What's here
- `pack.mcmeta` — pack metadata. **Confirm the `pack_format` for your exact 26.1.x
  build** with the misode generator (<https://misode.github.io/pack-mcmeta/>); the
  `supported_formats` range here is intentionally wide so it still loads.
- `assets/minecraft/items/carrot_on_a_stick.json` — a `range_dispatch` on
  `custom_model_data` that maps each ability item's value to its model:
  | cmd | item |
  |----|------|
  | 2 | Storm Scepter |
  | 3 | Warden Wand |
  | 4 | Ender Wand |
  | 5 | Troll Staff |
  | 6 | Cloning Cane |
- `assets/minecraft/models/item/*.json` — simple `item/generated` models.
- `assets/minecraft/textures/item/*.png` — **placeholder** solid-colour icons.
  Replace these with real 16×16 textures; the file names must stay the same.

## Notes / caveats
- **Warden Crossbow (cmd 1):** not included here. Overriding `items/crossbow.json`
  would replace the vanilla crossbow's charged/pulling animation for *all*
  crossbows, so a proper model must replicate those conditions. Until then the
  Warden Crossbow uses the normal crossbow look (its ability still works).
- **Bedrock / Geyser:** Java resource packs are **not** read by Geyser. To show
  these models to Bedrock players you need a separate Bedrock pack plus a Geyser
  item-mappings file (<https://geysermc.org/wiki/geyser/custom-items/>). The item
  *mechanics* work for Bedrock players regardless — they just see the base item.

## Using it
Zip the **contents** of this folder (so `pack.mcmeta` is at the zip root) and either
drop it in the client's `resourcepacks/` folder, or host it and set
`resource-pack` / `resource-pack-sha1` in `server.properties` to push it to players.
