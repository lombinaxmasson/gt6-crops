# Crop-card compatibility

The ledger in `src/main/resources/data/gt6crops/crop_cards.json` contains the
61 cards copied from the CrucibleCraft GT6 ledger, plus the `weed` card and
vanilla crop/flower cards. GT material forms are resolved only after the
GT6CE addon `onRecipesReady` callback. A card is enabled when its harvest item
and, for a non-crossbreed-only card, its base seed both resolve.

## Intentionally conditional cards

These cards are retained in the data file but are skipped unless their
external dependency is present:

- `desert_nova`, `cerublossom`: Ars Magica / Ars Nouveau.
- `shimmerleaf`, `cinderpearl`: Thaumcraft.

## Cards that depend on GT6CE food content

The following cards use `gregtech:` food items and are enabled when the
corresponding GT6CE item is registered:

`rye`, `barley`, `oats`, `rice`, `lemon_plant`, `chili`, `tomato_plant`,
`red_grapes`, `white_grapes`, `green_grapes`, `purple_grapes`,
`blueberry_bush`, `gooseberry_bush`, `candleberry_bush`, `cranberries`,
`black_currants`, `white_currants`, `red_currants`, `blackberries`,
`raspberries`, `strawberries`, `onion`, `cucumber`, `peanuts`, `ananas`.

The four apple cards are deliberately handled separately:

- `red_apple_tree` uses `minecraft:apple`, matching GT6's alias for the red apple.
- `green_apple_tree`, `yellow_apple_tree`, and `dark_red_apple_tree` use
  `gregtech:apple`. GT6CE did not port the three colored apple items.

## Metal cards from CropsNH

`galvania` (zinc), `nickelback` (nickel), `bauxia` (aluminium),
`pyrolusium` (manganese), `titania` (titanium), and `scheelinium` (tungsten)
follow the CropsNH cards of the same name: tier, seed colors, parents, and
mutation-pool membership. They drop the metal's `plantGtBlossom`, which is
the form GT6 already names "Leaf" for Ferru, Aurelia, Argentia, and
Plumbilia. CropsNH has no chromium crop; `chromia` is this addon's own card,
bred from Titania and Ferru.

CropsNH limits Bauxia, Titania, and Scheelinium to its crop breeder. Here
they breed in the world instead, marked `stand_in` in `mutations.json`:

- Bauxia keeps the CropsNH parents, Galvania and Nickelback.
- Titania replaces Red Straw with that crop's own parents: Bauxia, nether
  wart, and wheat.
- Scheelinium replaces the End Stone Lily with Ender Bloom: Titania,
  Pyrolusium, and Ender Bloom.

The breeder-only cards and Chromia belong to no mutation pool, so only their
listed parents produce them. The seven cards reuse the CC0 Argentia,
Plumbilia, and Indigo stage textures until they get art of their own.

## Growth conditions

The soil, sub-soil, light, and liked-biome fields follow the CropsNH card of
the same crop. CropsNH soil groups map to `gt6crops:soil/*` tags, and its
BiomeDictionary types map to the NeoForge `c:` biome tags (MESA is
`c:is_badlands`, CONIFEROUS is `c:is_tree/coniferous`, SPARSE and DENSE are
`c:is_sparse_vegetation` and `c:is_dense_vegetation`).

Cards without a CropsNH counterpart take the default of the CropsNH class they
would belong to:

- Food crops need farmland and light 9: `rye`, `oats`, `rice`, the four apple
  trees, `gooseberry_bush`, `candleberry_bush`, `cranberries`, the three
  currants, `peanuts`, `ananas`, and `beetroot`.
- `mint`, `desert_nova`, and `cerublossom` need farmland only.
- `cornflower` and `lily_of_the_valley` need dirt or grass, like the other
  vanilla flowers.

Differences from CropsNH:

- Each sub-soil tag also accepts the GT6CE ores of its metal, for example
  hematite, limonite, magnetite, and pyrite for iron, and cassiterite for tin.
  CropsNH lists fewer ores.
- `chromia` needs stone over chromite or a chromium block.
- `shimmerleaf` has no sub-soil. CropsNH asks for a quicksilver block, which
  GT6CE does not have.
- `corpse_plant` grows on soul sand or soul soil, the CropsNH graveyard soil
  when Tinkers' Construct is absent.
- A liked biome adds 25% growth speed, up to two of them. CropsNH adds 14
  nutrients per liked biome instead.

GT material names in the ledger are matched without case or underscores, so
`live_root` resolves to `LiveRoot` and `ender_pearl` resolves to `EnderPearl`.

The authoritative result for a particular GT6CE jar is printed during startup
as `loaded ... crop cards; skipped ...`, with one warning per skipped card.
