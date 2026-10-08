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
  Oil berries use an in-world stand-in (wither reed and nether wart) because
  CropsNH gates that crop behind the crop breeder;
- weeds, nutrient/humidity/air-quality effects, water, fertilizer, and Weed-EX;
- GT plant-form mortar and shredder integration where GT6CE has no conflicting
  input row.

The card compatibility notes are in
[`docs/crop-compatibility.md`](docs/crop-compatibility.md). The proposed GT6CE
integration contract is in
[`docs/gt6ce-crop-api-request.md`](docs/gt6ce-crop-api-request.md).

## Asset provenance

The crop-stage textures under `assets/gt6crops/textures/block/crop/` were
copied from the referencable GT6 port asset catalog. That catalog dedicates
its default assets to the public domain under CC0-1.0; the source notices
remain in the reference project.

The crop-stick model, its wood texture, the Weed-EX and crop-stick item
icons, and the four weed stages are original to this addon. Crop seeds reuse
the CropsNH wheat-seed body and highlight masks, tinted per crop.

## Development

Set `JAVA_HOME` to the project JDK 21 before using Gradle. Do not commit the
GT6CE jar; `libs/gt6ce/*.jar` is ignored. `build-commit.txt` and the checksum
record the GT6CE build used locally.
