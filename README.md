# GT6 Crops

NeoForge 1.21.1 crop sticks and CropsNH-style crossbreeding for GT6CE.

The addon uses the GT6CE Addon API v1 and keeps all GT6 content in the
`gregtech` dependency. The GT6CE jar is a development/runtime dependency only;
it is not bundled into the addon jar.

## Current mechanics

- crop sticks, which become a cross when a second stick is placed on an empty one;
- data-driven GT6 crop cards plus vanilla crops and flowers;
- one crop seed item whose name and growth/gain/resistance come from the
  stack, plus the original base seed for crops that have one;
- CropsNH-style breeding: explicit mutations, mutation pools, and parent-stat
  averaging. Fertilizer on every parent keeps the new stats from dropping.
  Oil berries, Bauxia, Titania, and Scheelinium use in-world stand-ins
  because CropsNH gates them behind the crop breeder;
- weeds, nutrient/humidity/air-quality effects, water, fertilizer, and Weed-EX;
- CropsNH-style growth conditions: each crop needs its own soil, some need a
  particular block under the soil or a minimum light level, and liked biomes
  speed growth up;
- GT's crop scanner reads planted crops through GT6CE's `CropScanSource`, and
  right-clicking a crop stick with a GT magnifying glass prints the crop's
  stats, storage, environment, and anything that stops it from growing;
- GT plant-form mortar and shredder integration where GT6CE has no conflicting
  input row.

The card compatibility notes are in
[`docs/crop-compatibility.md`](docs/crop-compatibility.md). The proposed GT6CE
integration contract is in
[`docs/gt6ce-crop-api-request.md`](docs/gt6ce-crop-api-request.md).

## Customizing crops

Every crop species, breeding rule, and seed tint is plain JSON. Adding a
crop, rewiring the breeding tree, or changing what a crop drops needs no Java,
and a modpack can do all of it without rebuilding the mod.

| File | Contents |
|---|---|
| `crop_cards.json` | crop cards |
| `mutations.json` | fixed mutations and mutation pools |
| `seed_colors.json` | seed tints |

The built-in copies are in the mod jar under `data/gt6crops/`; the jar is a
zip, so they can be copied out as a starting point. A file of the same name in
`config/gt6crops/` replaces the built-in copy as a whole, and the startup log
says `using <file> from <path>` for each one it picks up. Files that are not
there fall back to the built-in copy, so a pack can override only
`mutations.json`, for example.

The files are read once at startup, so restart the game after editing them;
`/reload` does not pick them up. Cards are not synced over the network, so a
server and its clients need the same files.

New growth-stage sprites go in a resource pack, under
`assets/gt6crops/textures/block/crop/<texture>/`.

### Crop cards

```json
{
  "id": "galvania",
  "name": "Galvania",
  "discovered_by": "CropsNH",
  "tier": 6,
  "max_size": 4,
  "growth_speed": 0,
  "after_harvest_size": 1,
  "harvest_size": 4,
  "stats": [2, 0, 0, 0, 0],
  "attributes": ["Bush", "Metal", "Zinc"],
  "crossbreed_only": true,
  "drop": {"kind": "gt_form", "prefix": "plantGtBlossom", "material": "zinc", "count": 1},
  "special_drops": [],
  "base_seed": {"kind": "none", "count": 1},
  "soil": ["#gt6crops:soil/stone"],
  "sub_soil": ["#gt6crops:sub_soil/zinc"],
  "liked_biomes": ["#c:is_dry", "#c:is_hot"],
  "texture": "argentia"
}
```

- `id` is the key used by mutations, pools, and seed colors. Seeds and planted
  crops store it, so renaming an id orphans the ones already in a world.
- `name` is the seed's display name.
- `tier` raises starting stats by one per four tiers, up to four. When
  `growth_speed` is `0`, each stage takes `tier × 200` growth points.
- `max_size` is the number of stages (at least 3). The crop can be harvested
  from `harvest_size` and drops back to `after_harvest_size`. It can cross or
  breed once it reaches 80% of `max_size`.
- `crossbreed_only: true` means the crop only comes from breeding. Otherwise
  right-clicking a crop stick with `base_seed` plants it.
- `drop` is the harvest. Each entry in `special_drops` has a one-in-four chance
  per harvest.
- `texture` names the sprite folder and defaults to the id without
  underscores. Several cards can share one folder.
- `render_shape` (optional) is `hash`, the default, for two sheets each way
  like vanilla crops, or `x` for two diagonal sheets like vanilla flowers.
  The cards CropsNH draws as an X use `x`.
- `blocked_without` (optional) is a mod id; the card is skipped unless that mod
  is loaded.
- `soil`, `sub_soil`, `min_light`, `max_light`, and `liked_biomes` (all
  optional) are the growth conditions described below.
- `stats`, `attributes`, and `discovered_by` are CropsNH metadata. The rules do
  not read them.

`drop`, `special_drops`, and `base_seed` are item references:

- `{"kind": "vanilla", "item": "minecraft:wheat_seeds"}` takes any registered
  item id. `item`, `gt_item`, and `foods_addon` work the same way.
- `{"kind": "gt_form", "prefix": "plantGtBerry", "material": "tin"}` is a GT
  material form. The prefixes are `plantGtBerry`, `plantGtBlossom`,
  `plantGtFiber`, `plantGtTwig`, `plantGtWart`, `dust`, `dustTiny`, `nugget`,
  `chunkGt`, `ingot`, and `stick`. Material names are GT6CE names with case,
  `_`, `-`, and spaces ignored, so `live_root` finds `LiveRoot`. Any GT6CE
  material works, and so does switching a crop from blossoms to berries.
- `{"kind": "none"}` is nothing. `count` defaults to 1.

A card that cannot load is skipped and logged as
`skipping unavailable crop card <id>`. That happens when its drop does not
resolve, when a card that is not `crossbreed_only` has no base seed that
resolves, or when `blocked_without` names a missing mod. Mutations and pools
ignore skipped cards. The startup line `loaded N crop cards; skipped M` gives
the totals.

### Growth conditions

The condition fields are lists. Each entry is a block or biome id, or a tag
written as `#namespace:path`, and one matching entry is enough.

- `soil` is the block the crop stick stands on. It defaults to
  `#gt6crops:soil/farmland`. A crop stick can stand on any block that some
  loaded card accepts as soil, but a seed is only planted on its own soil;
  otherwise the player is told which soil it needs.
- `sub_soil` is the block under the soil. Without it the crop does not grow.
- `min_light` and `max_light` default to 0 and 15. The light is measured at
  the crop stick, from torches or the sky, as vanilla crops do, so open sky
  counts even at night.
- `liked_biomes` speeds growth up by 25% for each matching biome tag, counting
  at most two.

A crop whose soil, sub-soil, or light is wrong keeps its size and can still be
harvested, but does not grow. A cross also checks the child's conditions at
its own position, and grows nothing when they fail.

The soils and sub-soils are block tags under `data/gt6crops/tags/block/soil/`
and `sub_soil/`, so a datapack can change what counts, and `/reload` picks the
change up. Each sub-soil tag accepts the ores and storage blocks of its metal
under both the `forge:` tags GT6CE generates and the `c:` tags vanilla and
other mods use. When a card's soil or sub-soil matches no block, the server
log warns that the crop can never grow. Liked biomes use the NeoForge `c:`
biome tags.

### Breeding

`mutations.json` has two lists. `mutations` are fixed recipes:

```json
{"child": "galvania", "parents": ["tine", "ferru"], "machine_only": false}
```

`parents` needs at least two cards and may hold more. Entries with
`machine_only: true` are kept for a future crop breeder and never fire in the
world.

`pools` are loose groups:

```json
{"id": "metallic", "crops": ["ferru", "coppon", "galvania"]}
```

When a cross tries to grow a crop, it looks at the four horizontal neighbors
that have reached 80% of their size:

1. Half the time it copies one of them.
2. Otherwise it takes the distinct card ids of those neighbors, at least two,
   and sorts them by their order in `crop_cards.json`. A mutation fires when
   its parents are exactly the first ids in that sorted list. So a neighbor
   listed earlier in `crop_cards.json` than the parents blocks the mutation,
   and one listed later does not. If several mutations match, one is picked
   at random.
3. If no mutation fires, any pool that holds two of the neighbors gives a
   random member of that pool. Pools have no tier limit, so a pool that mixes
   cheap and expensive crops is a shortcut to the expensive ones.
4. The child's growth, gain, and resistance are the average of the parents
   that took part, plus −2 to +4. Fertilizer on every one of those parents
   removes the negative half.

### Seed colors and textures

`seed_colors.json` maps a card id to `[body, highlight]` as hex RGB. A card
without an entry falls back to a color derived from its id. In this
repository `gradlew test` fails until every built-in card has one.

Stage sprites live in `assets/gt6crops/textures/block/crop/<texture>/` as
`1.png` through `<max_size>.png`. Draw only the plant; the crop sticks are
part of the block model.

Wheat, carrot, potato, beetroot, and nether wart use their vanilla stage
textures. Cocoa, pumpkin, and melon use CropsNH's stages. Flower and sugar cane
stages, and the fruit on a ripe pumpkin or melon, are drawn while textures load,
from the player's own vanilla textures, by the `gt6crops:crop_stages` source in
`assets/minecraft/atlases/blocks.json`. A resource pack can replace any stage by
providing `block/crop/<texture>/<stage>.png`.

### Processing

A card whose drop is a `plantGt*` form also gets a mortar row and a shredder
row that turn the drop into tiny dust of its material. Neither row is added
when GT6CE already has a mortar row for that input, and the shredder row is
also left out when GT6CE already has a shredder row for it.

### Adding a crop

1. Add a card to `crop_cards.json`, usually with `crossbreed_only: true` and a
   `gt_form` drop, and give it a soil and, if it needs one, a sub-soil.
2. Add a mutation with the card as `child`, and add it to pools if it should
   also come from them.
3. Add its seed colors.
4. Point `texture` at an existing folder or add stage sprites.
5. Restart the game and check the startup log for skipped cards. When editing
   the built-in copies in this repository, run `gradlew build` first; it also
   runs the tests.

## Asset provenance

The crop-stage textures under `assets/gt6crops/textures/block/crop/`, apart
from `cocoa/`, `pumpkin/`, and `melon/`, were copied from the referencable GT6
port asset catalog. That catalog dedicates
its default assets to the public domain under CC0-1.0; the source notices
remain in the reference project. The original art paints crop sticks into
every stage, as IC2 did; those pixels were erased because this addon draws
the sticks as a model.

The crop-stick model, its wood texture, the Weed-EX and crop-stick item
icons, and the four weed stages are original to this addon.

The crop-seed body and highlight masks (CropsNH's `genericSeed/vanilla1` and
`vanilla2`, tinted per crop), the cocoa, pumpkin, and melon stages, and the
pumpkin and melon vines in `textures/crop_parts/` come from CropsNH. They are
copyright the GTNH Team and licensed under LGPL-3.0-or-later. CropsNH's ripe
pumpkin and melon reuse pixels of Minecraft's textures, so those pixels were
removed and are redrawn from the player's own textures. The notice listing every
file and both licence texts are in `src/main/resources/META-INF/licenses/` and
ship in the jar.

Flower and sugar cane stages are not shipped at all; they are drawn from the
player's vanilla textures when the game loads.

## Development

Set `JAVA_HOME` to the project JDK 21 before using Gradle. Do not commit the
GT6CE jar; `libs/gt6ce/*.jar` is ignored. `build-commit.txt` and the checksum
record the GT6CE build used locally. CI builds GT6CE from the commit in
`build-commit.txt` and caches the jar by that commit, so updating GT6CE locally
and committing the new `build-commit.txt` moves CI with it.
