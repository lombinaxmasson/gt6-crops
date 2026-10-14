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
- weeds, nutrient/humidity/air-quality effects, water, fertilizer, and Weed-EX.
  Fertilizer is anything in the `gt6crops:fertilizer` item tag, which holds
  bone meal by default;
- CropsNH-style seed returns: a harvest gives only the produce. Right-clicking
  with a GT spade or universal spade digs a crop up at any size and returns
  its seed with the same stats, plus the harvest if it is ripe. It also clears
  weeds of any size, and a fully grown weed drops a tuft of short grass. Each
  use costs one point of durability. Breaking a planted stick drops the
  harvest if it is ripe, but keeps the seed only when the crop's resistance
  beats a roll from 0 to 30;
- rain waters crops under open sky in biomes where it rains, adding 4 of 32
  water every growth cycle (256 ticks);
- CropsNH-style disease. Each cycle a growing crop gets a CropsNH nutrient
  score: 5, up to 10 each from stored water and fertilizer, 2 under open sky,
  and 14 per liked biome (at most two) or up to 14 in a humid biome, whichever
  is more. A crop whose score is far below what its tier needs starves: it does
  not grow, and falls sick unless it passes the resistance roll. Under open
  sky in a dry biome, tier 6 and up starve with empty storage, and tier 9 and
  up starve even with the starting 8 water and 8 fertilizer; without sky each
  limit is one tier lower. A sick crop does not grow, and each cycle
  exposes one random neighbouring crop, which escapes on a resistance roll or
  by spending 2 stored Weed-EX. Plant Cure (100 uses) heals a sick crop and
  tops its fertilizer up to 8. Digging, breaking, or replanting a crop also
  ends the disease. As in CropsNH, a sick crop's sticks turn pale green and
  its plant keeps only the green of its texture;
- CropsNH-style weeds. An empty single stick grows a weed one cycle in 100.
  Weeds grow whatever their conditions. A weed that is half grown, and any
  other crop that is fully grown with growth above 24 and resistance below its
  growth, spreads weeds on cycles where a roll from 0 to 49, minus its growth,
  is 2 or less. Each spread picks one random side. An empty stick, single or
  cross, gets a weed; a planted crop is exposed to disease as above, so
  resistance 31 is immune; and air over grass, dirt, or farmland turns into
  short grass on a grass block. Stored Weed-EX keeps a weed out of a stick by
  spending 5. It is only spent when it blocks a weed or disease. Weeds cannot
  be harvested; clear them with a spade or Weed-EX, or break the stick;
- CropsNH-style trampling. Each tick a player or mob sprints through a planted
  crop, it tramples the crop with a chance of 1 in 100 × 0.95^tier (1 in 95 at
  tier 1, 1 in 59 at tier 10), and only when a roll from 0 to 30 beats the
  crop's resistance. Landing on a crop from more than 0.75 blocks tramples it
  with a chance of the fall height minus 0.75, so a fall of 1.75 blocks or
  more always does.
  A trampled stick breaks as if mined, and farmland under it turns to dirt.
  Weeds, empty sticks, crops with resistance 31, and crops on a soil in the
  `gt6crops:trample_proof_soil` block tag are never trampled. That tag is
  empty by default;
- CropsNH-style seed scanning. A crossbred crop starts unscanned, and a seed
  dug up from it reads "Unknown Seed" with no stats. Scanning the crop, with
  GT's crop scanner or by right-clicking it with a magnifying glass, reveals
  both. Base seeds, weeds, and crops and seeds from before this existed count
  as scanned. GT6CE has no machine that scans seed items, so a seed can only
  be scanned by planting it first;
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

## Machines

The Addon API cannot add GT machine types, so the machines are this addon's own
blocks that take EU from GT6CE cables the way GT6CE's machines do. A packet
below half the machine's voltage is lost, and one above twice its voltage
makes it explode (see `machines.overvoltage_explosions`). A machine stores no
energy: it only works on ticks when at least half its voltage arrives.

Right-clicking a machine opens its screen, and right-clicking it with a bucket
or other fluid container pours into its tanks. From any side, hoppers and pipes
can fill the input slots and tanks and empty the output slots.

Both machines come in GT6CE's five lowest electric tiers, LV to IV, each
tinted like that tier's machine casing.

### Crop Manager

The Crop Manager tends the crop sticks around it, as CropsNH's does. Its reach,
tanks, and harvest bonus grow with its tier, as in CropsNH:

| Tier | Reach (horizontal, vertical) | Harvest bonus | Water | Fertilizer | Weed-EX |
|---|---|---|---|---|---|
| LV | 5, 2 | 5% | 32,000 mB | 36,864 mB | 1,500 mB |
| MV | 7, 2 | 10% | 64,000 mB | 73,728 mB | 3,000 mB |
| HV | 9, 2 | 15% | 96,000 mB | 110,592 mB | 4,500 mB |
| EV | 11, 2 | 20% | 128,000 mB | 147,456 mB | 6,000 mB |
| IV | 13, 2 | 25% | 160,000 mB | 184,320 mB | 7,500 mB |

The rest of this section uses the LV numbers. After 50 powered ticks in a
row it runs one round; a tick without power starts the count over. Four
switches on its screen choose what a round does, and only harvesting is on
when it is placed:

- Harvesting puts ripe crops' produce in its 18 output slots, 5% more on
  average than a hand harvest. Weeds are not harvested. When the output slots
  are full it waits; drops that did not fit are kept, go out first next round,
  and drop if the machine is broken.
- Watering tops up a stick with 28 or less of 32 water from its 32,000 mB tank,
  at 6 mB per point.
- Fertilizing works the same way from its 36,864 mB liquid fertilizer tank.
  Enriched fertilizer piped in counts as ten times as much liquid fertilizer.
  While the tank holds less than 6 mB, a stick with 28 or less of 32 fertilizer
  instead gets one item from its fertilizer slots (anything in
  `gt6crops:fertilizer`), worth 8 as by hand.
- Weed-EX is GT6CE's strong poison potion, as in CropsNH, held in a 1,500 mB
  tank. Weed-EX items in its two Weed-EX slots pour 100 mB each into it. A stick
  with 12 or less Weed-EX gets 12 for 10 mB. As in CropsNH, this does not clear
  a weed that is already growing.

Watering, fertilizing, and Weed-EX reach every stick in range, including empty
sticks and weeds. Liquid and enriched fertilizer are this addon's fluids. Like
CropsNH's, they have no block or bucket and only move through pipes and tanks.
GT6CE machines make them from CropsNH's recipes, at 16 EU/t. CropsNH makes
fertilizer items and extracts 144 mB from each; this addon has no fertilizer
item, so its Mixer recipes make the liquid directly:

- In the Mixer, one calcite, calcium, apatite, or glauconite dust with one
  sulfur, phosphorite, or phosphate dust, three ash dusts, or one dark ash dust,
  and 1,000 mB of water, make 144 mB of liquid fertilizer for each item CropsNH
  would make: 1 to 3 from calcite and 2 to 4 from the others. Phosphorite takes
  the place of CropsNH's tricalcium phosphate. Each takes 5 seconds per 144 mB.
- In the Mixer, a dirt block, two wood dusts, four sand, and 1,000 mB of water
  make 576 mB in 5 seconds.
- In the Squeezer, a bone meal makes 48 mB, as much as it is worth by hand.
- In the Mixer, four bone meal and 1,000 mB of liquid fertilizer make 1,000 mB of
  enriched fertilizer in 1 second.

Water can be any of the four GT6CE's own Mixer recipes take: plain, mineral,
or distilled water, or spectral dew. A recipe is left out, with a warning in the log, if GT6CE
lacks one of its dusts or already has a recipe those inputs would run.

The Crop Manager's recipe is CropsNH's with GT6CE parts of its tier: two robot
arms around a sensor, plates on either side of a machine casing, and cables on
either side of a medium fluid pipe, which takes the place of CropsNH's input
hatch.

| Tier | Plates and casing | Cable | Pipe | Circuit (Seed Generator) |
|---|---|---|---|---|
| LV | galvanized steel | tin | galvanized steel | basic |
| MV | aluminium | copper | aluminium | good |
| HV | stainless steel | gold | stainless steel | advanced |
| EV | chromium | aluminium | chrome | elite |
| IV | titanium | platinum | titanium | master |

### Seed Generator

The Seed Generator copies a scanned crop seed, as CropsNH's does. A seed in
either input slot is kept, and a copy with the same species, stats, and scan
goes to the output slot. Unscanned seeds and weeds are not copied.

- A copy costs 36 mB of enriched fertilizer per stat point (growth, gain, and
  resistance added up), at least 1 mB. Its tank only takes enriched
  fertilizer. As in CropsNH, the LV to EV tanks are a tenth of a GT5u basic
  machine's: 1,600, 3,200, 6,400, and 12,800 mB, and the IV tank holds
  256,000 mB. A maxed seed costs 3,348 mB, so the LV generator only copies
  seeds of up to 44 total stats and the MV one up to 88.
- Some crops also take a catalyst from the input slots, the first of the
  card's `duplication_catalysts` the slots hold enough of. Metal crops take a
  dust of their metal or its ore, for example, and Diareed a diamond.
- A copy is CropsNH's LV recipe, 30 EU/t for 8 seconds, overclocked as GT6CE
  overclocks its own recipes on higher tiers. The fertilizer and catalyst are
  spent when the copy starts, and it only starts if the output slot has room.
  As in GT6CE, a tick with too little power starts the copy over, but nothing
  spent is lost.

Its recipe is CropsNH's: a GT6CE spade, a robot arm, and a water bucket on
top, the tier's circuits on either side of its machine casing, and its cables
on either side of a dirt block. The parts are those in the table above.

## Recipe viewer

EMI is optional and is not packed into this jar. When it is installed, the crop
stick is the workstation for three tabs. The pages use the slot diagrams under
`textures/gui/nei/`. EMI's craft tree leaves them out.

- **Planting** is CropsNH's crop page, one per crop, weeds aside. The left
  column is the crop seed, the soil, and, when the card has one, the block
  under the soil. The right side is the harvest, three columns, the main drop
  first. Each drop carries CropsNH's yellow chance in its corner: the main drop
  reads 100.00%, and each special drop reads 25.00%. The arrow is painted on
  the sheet. Under the panel: tier, growth points for a stage, liked biomes,
  light when it is not the full 0-15 range, and how the seed is planted. A crop
  with a base seed also gets CropsNH's alternate-seed page in this same tab:
  the base seed on the left, the crop seed on the right.
- **Crossbreeding** is one page per fixed mutation. Two parents use
  `breeding2.png`; three or four use `breeding4.png`. The parents sit on the
  arms and stay in the ground. The child seed is the center slot, and the soil
  and subsoil under it are the child's. The harvest stays on the planting page.
  The lines under the panel are the world rule: 80% size, the try rate, the
  half of attempts that copy a neighbor, the ledger order that lets an earlier
  neighbor block the cross, and the stat average. A `machine_only` mutation is
  still listed, with a line that a crop stick never performs it.
- **Crossbreeding Pool** is one page per pool. Members fill nine columns inside
  a frame tiled from `mutation_pool.png`. The name under the frame comes from
  `emi.gt6crops.pool.<id>` (a datapack pool with no translation falls back to
  its id with the underscores turned into words). Two mature members can give
  any member, and only when no fixed cross matched.

The try rate and the stat range printed on the cross and pool pages are this
client's `config/gt6crops-common.toml`. A dedicated server uses its own copy
for the crosses that actually happen.

Looking up a crop seed finds every page that grows it, uses it, or gives it.
Seeds of one crop match each other whatever their growth, gain, and resistance
are. The seed drawn on a page hides those numbers, because they are not the
result of the cross or of planting.

### Page sheets

These five files are the slot diagrams, under `assets/gt6crops/textures/gui/nei/`.
Redraw them in place. Keep the file name, the pixel size, and every gray hole
where it is. The code puts each 16×16 icon on the hole whose top-left is in
the table. Text under a diagram is squeezed horizontally to 166 pixels when a
line is wider than that, and it is not wrapped. The color is `0x191616`.

| File | Size | Icons, top-left of the 16×16 hole |
|---|---|---|
| `breeding2.png` | 80×75 | Parents (1, 16) and (63, 16). Child (32, 16). Soil (32, 37). Subsoil (32, 58). Lines start at y=77. |
| `breeding4.png` | 122×75 | Parents (22, 16), (84, 16), (1, 16), (105, 16). Child (53, 16). Soil (53, 37). Subsoil (53, 58). |
| `crop_list.png` | 140×75 | Seed (16, 16). Soil (16, 37). Subsoil (16, 58). Drops from (87, 12), step 18, three columns. The arrow is painted between the columns. Lines start at y=77. |
| `mutation_pool.png` | 162×38, tiled | Top cap is the first 9 rows. Each extra row uses rows 10–27. The bottom cap is the last 9 rows. Icons from (1, 1), step 18, nine columns. The name is at (0, rows×18+2). |
| `alternate_seeds.png` | 59×18 | Base seed (1, 1). Crop seed (42, 1). The arrow between them is painted on the sheet. |

### Optional tab icons

Until the file below exists, that tab uses the crop stick item. A resource
reload picks a new file up. Do not add a blank or fully transparent file:
a file that exists replaces the stick, even if it draws nothing. These are the
tab buttons, not the pages above.

Each file is a 16×16 PNG under `assets/gt6crops/textures/gui/emi/`, with a
transparent background. Paint pixels, not anti-aliased strokes: alpha is only
fully off or fully on. Leave the outer pixel empty so EMI's tab highlight does
not clip the drawing. EMI does not tint the icon, so the colors you paint are
the colors it shows. Use the oak of `textures/item/crop_stick.png`. The block
texture `textures/block/crop_stick.png` is a wood tile the model stretches
onto the stick; do not crop that tile into the icon.

| File | What to draw |
|---|---|
| `planting.png` | One upright crop stick, not the diagonal item icon. A sprout of about three green pixels at the top, and a two-pixel brown soil line at the bottom. This is the Planting tab. |
| `crossbreeding.png` | The cross as it stands in the world: one upright stick, and a second stick across its upper half. No leaves and no seeds. This is the Crossbreeding tab. |
| `mutation_pool.png` | Four separate 2×2 seed marks, each an oak body with one green pixel, spaced so they read as a group. No stick. This is the Crossbreeding Pool tab. |

## Configuration

`config/gt6crops-common.toml` holds CropsNH's global settings. It can also be
edited from the mod list's Config button. The server reads them during growth
cycles, so a dedicated server uses its own copy.

| Setting | Default | Meaning |
|---|---|---|
| `crops.growth_multiplier` | 1.0 | multiplies growth per cycle, from 0 to 2 |
| `crops.breeding_chance` | 3 | an empty cross tries to crossbreed one cycle in this many |
| `crops.breeding_low` | -2 | lowest change to each crossbred stat |
| `crops.breeding_high` | 4 | highest change to each crossbred stat |
| `weeds.enable_weeds` | true | turns weed growth and spreading on or off |
| `weeds.weed_spawn_chance` | 100 | an empty single stick grows a weed one cycle in this many |
| `weeds.weed_spread_chance` | 50 | the weed-spread roll runs from 0 to this minus 1 |
| `machines.overvoltage_explosions` | true | a machine fed more than twice its voltage explodes, as GT6CE's do; when off, the packet is lost |

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
- `duplication_catalysts` (optional) lists alternatives for the Seed
  Generator to spend when it copies the seed, each with its own `count`;
  it takes the first the input slots hold enough of. Entries that do not
  resolve are left out, and a card that lists catalysts of which none resolve
  cannot be copied, with a warning in the log. Without the field, copies need
  only fertilizer.
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

`drop`, `special_drops`, `base_seed`, and `duplication_catalysts` are item references:

- `{"kind": "vanilla", "item": "minecraft:wheat_seeds"}` takes any registered
  item id. `item`, `gt_item`, and `foods_addon` work the same way.
- `{"kind": "gt_form", "prefix": "plantGtBerry", "material": "tin"}` is a GT
  material form. The prefixes are `plantGtBerry`, `plantGtBlossom`,
  `plantGtFiber`, `plantGtTwig`, `plantGtWart`, `dust`, `dustTiny`, `gem`,
  `nugget`, `chunkGt`, `ingot`, and `stick`. Material names are GT6CE names with case,
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
   that took part, plus −2 to +4 by default (see
   [Configuration](#configuration)). Fertilizer on every one of those parents
   removes the negative half. The child is unscanned. With EMI installed, the same
rules are the Crossbreeding, Planting, and Crossbreeding Pool tabs; see
[Recipe viewer](#recipe-viewer).

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
icons, the four weed stages, the machine overlays, and the machine
screen are original to this addon. The Plant Cure
icon is the Weed-EX icon with its greens recoloured.

Machines are drawn as GT6CE draws its own: GT6CE's metallic machine casing,
tinted with the colour of the tier's casing material (galvanized steel for
LV, then aluminium, stainless steel, chromium, and titanium), under this
addon's overlays. The casing texture is read from the
installed GT6CE jar and is not shipped here.

The crop-seed body and highlight masks (CropsNH's `genericSeed/vanilla1` and
`vanilla2`, tinted per crop), the cocoa, pumpkin, and melon stages, and the
pumpkin and melon vines in `textures/crop_parts/` come from CropsNH. The five
EMI page sheets in `textures/gui/nei/` are the slot diagrams cropped out of
CropsNH's NEI sheets. They are
copyright the GTNH Team and licensed under LGPL-3.0-or-later. CropsNH's ripe
pumpkin and melon reuse pixels of Minecraft's textures, so those pixels were
removed and are redrawn from the player's own textures. The notice listing every
file and both licence texts are in `src/main/resources/META-INF/licenses/` and
ship in the jar.

Flower and sugar cane stages are not shipped at all; they are drawn from the
player's vanilla textures when the game loads.

## License

GT6 Crops is licensed under the GNU Lesser General Public License, version 3
or (at your option) any later version. `LICENSE` is the LGPL-3.0 text and
`COPYING` is the GPL-3.0 text it incorporates. The same two texts ship in the
jar as `META-INF/licenses/LGPL-3.0.txt` and `GPL-3.0.txt`. The mods list shows
the SPDX identifier `LGPL-3.0-or-later`.

The CropsNH files listed in `META-INF/licenses/CropsNH-NOTICE.txt` stay under
that same licence, copyright the GTNH Team. Crop-stage textures copied from
the GT6 reference catalog are CC0-1.0, as noted under Asset provenance.

## Development

Op level 2 and above get `/gt6crops` debug commands. Look at a crop stick and use
`tick [count]` to run growth cycles without waiting, `mature` or `setsize`,
`setstats <g> <ga> <r>`, `fill` for storage, or `plant <card> [g ga r]`. Vanilla
`/tick rate` also speeds the whole world, but the crop command is the one that
skips the 256-tick gate.

Set `JAVA_HOME` to the project JDK 21 before using Gradle. Do not commit the
GT6CE jar; `libs/gt6ce/*.jar` is ignored. `build-commit.txt` and the checksum
record the GT6CE build used locally. CI builds GT6CE from the commit in
`build-commit.txt` and caches the jar by that commit, so updating GT6CE locally
and committing the new `build-commit.txt` moves CI with it.
