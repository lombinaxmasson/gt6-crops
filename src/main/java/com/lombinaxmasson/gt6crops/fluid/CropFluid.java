package com.lombinaxmasson.gt6crops.fluid;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A fluid that, like CropsNH's, only lives in tanks and pipes: it has no block and
 * no bucket. It is drawn with vanilla's water texture in its own color.
 */
public final class CropFluid {
    private final int color;
    private final DeferredHolder<FluidType, FluidType> type;
    private final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
    private final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;

    public CropFluid(DeferredRegister<FluidType> types, DeferredRegister<Fluid> fluids, String name, int color) {
        this.color = color;
        type = types.register(name, () -> new FluidType(FluidType.Properties.create()));
        source = fluids.register(name, () -> new BaseFlowingFluid.Source(properties()));
        flowing = fluids.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(properties()));
    }

    private BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(type, source, flowing);
    }

    public FluidType type() {
        return type.get();
    }

    public Fluid source() {
        return source.get();
    }

    /** RGB. */
    public int color() {
        return color;
    }
}
