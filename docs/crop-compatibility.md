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

- `red_apple_tree` uses `minecraft:apple`.
- `green_apple_tree`, `yellow_apple_tree`, and `dark_red_apple_tree` point at
  their historical GT6 names and are skipped if GT6CE does not provide those
  items.

The authoritative result for a particular GT6CE jar is printed during startup
as `loaded ... crop cards; skipped ...`, with one warning per skipped card.
