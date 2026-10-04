# GT6CE crop integration request

This is a draft issue for SaltNya. `gt6-crops` intentionally does not put
classes in the `ic2` namespace and does not use reflection or mixins to make
its crop block look like an IC2 tile.

## Problem

GT6CE's crop-facing tools currently identify crops through the optional
`ic2.api.crops.ICropTile` contract. In particular, the crop analyzer,
Plantalyzer recipes, watering, Weed-EX, and crop-harvesting integrations cannot
see a NeoForge-native crop tile owned by an addon.

`gt6-crops` stores the following state in its own `CropTile`:

- crop-card id and display metadata;
- size;
- growth, gain, and resistance genetic values;
- nutrient, hydration, air-quality, fertilizer, water, and Weed-EX storage.

## Requested API shape

Please expose a small common crop capability/contract, for example:

```java
public interface GTCropTile {
    String cropId();
    int size();
    int growth();
    int gain();
    int resistance();
    int nutrientStorage();
    int hydrationStorage();
    int weedExStorage();
    boolean applyWater();
    boolean applyWeedEx();
    boolean harvest(boolean forced);
}
```

The exact names are up to GT6CE. The important part is that addons can expose
an adapter or capability without claiming `ic2.api.crops.ICropTile`.

GT6CE could then:

1. let `OriginalCropScan` query a public adapter and print the same six
   categories it already prints for IC2 crops;
2. let Plantalyzer accept a capability-backed crop input;
3. let watering, Weed-EX, and crop tools call the adapter;
4. keep the existing IC2 reflection path as a compatibility fallback.

An event or registry for crop adapters would also work if a direct capability
is not desirable. The contract should be common-side, avoid a mandatory IC2
dependency, and be stable across Forge 1.20.1 and NeoForge 1.21.1 where
possible.
