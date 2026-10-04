# GT6 Crops

NeoForge 1.21.1 crop sticks and IC2-inspired crossbreeding for GT6CE.

The addon uses the GT6CE Addon API v1 and keeps all GT6 content in the
`gregtech` dependency. The GT6CE jar is a development/runtime dependency only;
it is not bundled into the addon jar.

## Current mechanics

- crop and cross-crop sticks;
- data-driven GT6 crop cards plus vanilla crops and flowers;
- seed bags carrying species and growth/gain/resistance genetics in a 1.21
  custom-data component;
- IC2-style crossbreeding, weeds, nutrient/humidity/air-quality effects, water,
  fertilizer, and Weed-EX;
- GT plant-form mortar and shredder integration where GT6CE has no conflicting
  input row.

The card compatibility notes are in
[`docs/crop-compatibility.md`](docs/crop-compatibility.md). The proposed GT6CE
integration contract is in
[`docs/gt6ce-crop-api-request.md`](docs/gt6ce-crop-api-request.md).

## Asset provenance

The GT6 crop-stage textures under `assets/gt6crops/textures/block/crop/` were
copied from the referencable GT6 port asset catalog. That catalog marks its
default assets as CC0-1.0; the source notices and license files remain in the
reference project. The crop-stick and item icons are temporary placeholders
from the same crop texture set.

## Development

Set `JAVA_HOME` to the project JDK 21 before using Gradle. Do not commit the
GT6CE jar; `libs/gt6ce/*.jar` is ignored. `build-commit.txt` and the checksum
record the GT6CE build used locally.
