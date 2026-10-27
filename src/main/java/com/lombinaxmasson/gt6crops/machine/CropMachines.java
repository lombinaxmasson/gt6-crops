package com.lombinaxmasson.gt6crops.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gregtech.api.addon.machine.AddonMachine;
import gregtech.api.addon.machine.AddonMachineContributor;
import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.addon.platform.BehaviorMachineContributor;
import gregtech.api.addon.platform.MachineBehavior;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
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
    public List<AddonMachine> addonMachines() {
        List<AddonMachine> machines = new ArrayList<>();
        for (MachineTier tier : MachineTier.values()) {
            machines.add(cropManager(tier));
            machines.add(seedGenerator(tier));
        }
        return List.copyOf(machines);
    }

    private static BehaviorMachine cropManager(MachineTier tier) {
        BehaviorMachine.Builder builder = BehaviorMachine.builder(casing(CROP_MANAGER, tier))
                .blockEntityId(CROP_MANAGER)
                .energy(energy(tier))
                .gui(CROP_GUI)
                .guiSize(176, 208)
                .energyGauge(48, 17)
                .progress(133, 57);
        for (int slot = 0; slot < CropManagerBehavior.OUTPUT_START; slot++) {
            builder.item(8 + slot % 2 * 18, 18 + slot / 2 * 18, BehaviorMachine.SlotRole.INPUT);
        }
        for (int slot = 0; slot < CropManagerBehavior.OUTPUT_SLOTS; slot++) {
            builder.item(8 + slot % 9 * 18, 77 + slot / 9 * 18, BehaviorMachine.SlotRole.OUTPUT);
        }
        return builder
                .fluid(CropManagerBehavior.waterCapacity(tier), 62, 17, BehaviorMachine.SlotRole.INPUT)
                .fluid(CropManagerBehavior.fertilizerCapacity(tier), 82, 17, BehaviorMachine.SlotRole.INPUT)
                .fluid(CropManagerBehavior.weedExCapacity(tier), 102, 17, BehaviorMachine.SlotRole.INPUT)
                .toggle("gui.gt6crops.crop_manager.harvest", 126, 17, true)
                .toggle("gui.gt6crops.crop_manager.water", 146, 17, false)
                .toggle("gui.gt6crops.crop_manager.fertilizer", 126, 37, false)
                .toggle("gui.gt6crops.crop_manager.weed_ex", 146, 37, false)
                .build();
    }

    private static BehaviorMachine seedGenerator(MachineTier tier) {
        return BehaviorMachine.builder(casing(SEED_GENERATOR, tier))
                .blockEntityId(SEED_GENERATOR)
                .energy(energy(tier))
                .gui(SEED_GUI)
                .energyGauge(8, 17)
                .progress(103, 35)
                .item(62, 35, BehaviorMachine.SlotRole.INPUT)
                .item(80, 35, BehaviorMachine.SlotRole.INPUT)
                .item(134, 35, BehaviorMachine.SlotRole.OUTPUT)
                .fluid(SeedGeneratorRules.capacity(tier), 22, 17, BehaviorMachine.SlotRole.INPUT)
                .build();
    }

    private static MachineCasing casing(String machineType, MachineTier tier) {
        return MachineCasing.builder(machineType + "_" + tier.id(), tier.casingMaterial())
                .machineType(machineType)
                .tier(tier.index())
                .build();
    }

    /**
     * Rated EU for this tier. Leaving the packet range unset keeps GregTech's half-to-double
     * window, and the default still overloads a packet above that window.
     */
    private static EnergyInput energy(MachineTier tier) {
        return EnergyInput.builder(EnergyType.EU, tier.voltage()).build();
    }
}
