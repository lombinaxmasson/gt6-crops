package com.lombinaxmasson.gt6crops.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gregtech.gregtech.api.addon.AddonMachineContributor;
import com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration;
import com.gregtech.gregtech.api.addon.platform.BehaviorMachineContributor;
import com.gregtech.gregtech.api.addon.platform.MachineBehavior;
import com.lombinaxmasson.gt6crops.Gt6Crops;

/**
 * Crop Manager and Seed Generator tiers. GregTech builds the blocks, items and the one
 * block entity each machine shares. Block ids stay {@code crop_manager_<tier>} and
 * {@code seed_generator_<tier>}. The block entities stay {@code crop_manager} and
 * {@code seed_generator}, so an existing world still finds them.
 */
public final class CropMachines implements AddonMachineContributor, BehaviorMachineContributor {
    private static final String CROP_MANAGER = "crop_manager";
    private static final String SEED_GENERATOR = "seed_generator";
    private static final String CROP_GUI = "gt6crops:textures/gui/crop_manager.png";
    private static final String SEED_GUI = "gt6crops:textures/gui/machine.png";

    @Override
    public String addonId() {
        return Gt6Crops.MODID;
    }

    @Override
    public Set<String> machineTypes() {
        return Set.of(CROP_MANAGER, SEED_GENERATOR);
    }

    @Override
    public MachineBehavior create(String machineType) {
        return switch (machineType) {
            case CROP_MANAGER -> new CropManagerBehavior();
            case SEED_GENERATOR -> new SeedGeneratorBehavior();
            default -> throw new IllegalArgumentException("gt6crops has no behavior for " + machineType);
        };
    }

    @Override
    public List<BehaviorMachineDeclaration> behaviorMachines() {
        List<BehaviorMachineDeclaration> machines = new ArrayList<>();
        for (MachineTier tier : MachineTier.values()) {
            machines.add(cropManager(tier));
            machines.add(seedGenerator(tier));
        }
        return List.copyOf(machines);
    }

    private static BehaviorMachineDeclaration cropManager(MachineTier tier) {
        BehaviorMachineDeclaration.Builder builder = BehaviorMachineDeclaration.builder(
                        CROP_MANAGER + "_" + tier.id(), tier.casingMaterial())
                .machineType(CROP_MANAGER)
                .blockEntityId(CROP_MANAGER)
                .energy("EU", tier.voltage())
                .tier(tier.index())
                .gui(CROP_GUI)
                .guiSize(176, 208)
                .energyGauge(48, 17)
                .progress(133, 57);
        for (int slot = 0; slot < CropManagerBehavior.OUTPUT_START; slot++) {
            builder.item(8 + slot % 2 * 18, 18 + slot / 2 * 18, BehaviorMachineDeclaration.SlotRole.INPUT);
        }
        for (int slot = 0; slot < CropManagerBehavior.OUTPUT_SLOTS; slot++) {
            builder.item(8 + slot % 9 * 18, 77 + slot / 9 * 18, BehaviorMachineDeclaration.SlotRole.OUTPUT);
        }
        return builder
                .fluid(CropManagerBehavior.waterCapacity(tier), 62, 17, BehaviorMachineDeclaration.SlotRole.INPUT)
                .fluid(CropManagerBehavior.fertilizerCapacity(tier), 82, 17, BehaviorMachineDeclaration.SlotRole.INPUT)
                .fluid(CropManagerBehavior.weedExCapacity(tier), 102, 17, BehaviorMachineDeclaration.SlotRole.INPUT)
                .toggle("gui.gt6crops.crop_manager.harvest", 126, 17, true)
                .toggle("gui.gt6crops.crop_manager.water", 146, 17, false)
                .toggle("gui.gt6crops.crop_manager.fertilizer", 126, 37, false)
                .toggle("gui.gt6crops.crop_manager.weed_ex", 146, 37, false)
                .build();
    }

    private static BehaviorMachineDeclaration seedGenerator(MachineTier tier) {
        return BehaviorMachineDeclaration.builder(SEED_GENERATOR + "_" + tier.id(), tier.casingMaterial())
                .machineType(SEED_GENERATOR)
                .blockEntityId(SEED_GENERATOR)
                .energy("EU", tier.voltage())
                .tier(tier.index())
                .gui(SEED_GUI)
                .energyGauge(8, 17)
                .progress(103, 35)
                .item(62, 35, BehaviorMachineDeclaration.SlotRole.INPUT)
                .item(80, 35, BehaviorMachineDeclaration.SlotRole.INPUT)
                .item(134, 35, BehaviorMachineDeclaration.SlotRole.OUTPUT)
                .fluid(SeedGeneratorRules.capacity(tier), 22, 17, BehaviorMachineDeclaration.SlotRole.INPUT)
                .build();
    }
}
