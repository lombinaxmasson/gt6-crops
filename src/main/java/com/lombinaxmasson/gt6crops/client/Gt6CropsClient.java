package com.lombinaxmasson.gt6crops.client;

import org.jetbrains.annotations.Nullable;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.fluid.CropFluid;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.item.SeedColors;
import com.lombinaxmasson.gt6crops.machine.MachineBlock;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client crop models, generated crop stages, crop, seed and machine tints, machine screens,
 * fluid textures, and the config screen.
 */
@Mod(value = Gt6Crops.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Gt6Crops.MODID, value = Dist.CLIENT)
public final class Gt6CropsClient {
    /** CropsNH's sick colors: pale green sticks, and a plant that keeps only its green. */
    private static final int SICK_STICK_COLOR = 0xAAFFAA;
    private static final int SICK_PLANT_FILTER = 0x00FF00;
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

    public Gt6CropsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void registerSeedColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            String cardId = CropSeedItem.data(stack)
                    .map(CropSeedItem.SeedData::cardId)
                    .orElse(null);
            return SeedColors.argb(cardId, tintIndex);
        }, Gt6Crops.CROP_SEED.get());
    }

    @SubscribeEvent
    public static void registerCropStages(RegisterSpriteSourceTypesEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "crop_stages"), CropStageSprites.TYPE);
    }

    @SubscribeEvent
    public static void registerCropTints(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> switch (tintIndex) {
            case CropPlantModel.GRASS_TINT -> grassColor(level, pos);
            case CropPlantModel.SICK_STICK_TINT -> SICK_STICK_COLOR;
            case CropPlantModel.SICK_PLANT_TINT -> SICK_PLANT_FILTER;
            case CropPlantModel.SICK_GRASS_TINT -> grassColor(level, pos) & SICK_PLANT_FILTER;
            default -> -1;
        }, Gt6Crops.CROP_STICK.get());
    }

    @SubscribeEvent
    public static void registerMachineScreens(RegisterMenuScreensEvent event) {
        event.register(Gt6Crops.CROP_MANAGER_MENU.get(), CropManagerScreen::new);
    }

    @SubscribeEvent
    public static void registerFluidTextures(RegisterClientExtensionsEvent event) {
        for (CropFluid fluid : new CropFluid[] {Gt6Crops.FERTILIZER, Gt6Crops.ENRICHED_FERTILIZER}) {
            int tint = 0xFF000000 | fluid.color();
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return WATER_STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return WATER_FLOW;
                }

                @Override
                public int getTintColor() {
                    return tint;
                }
            }, fluid.type());
        }
    }

    /** Machine casings are greyscale and take their tier's material color, as in GT6CE. */
    @SubscribeEvent
    public static void registerMachineTints(RegisterColorHandlersEvent.Block event) {
        Block[] machines = machineBlocks();
        if (machines.length > 0) {
            event.register((state, level, pos, tintIndex) -> casingColor(state.getBlock(), tintIndex), machines);
        }
    }

    @SubscribeEvent
    public static void registerMachineItemTints(RegisterColorHandlersEvent.Item event) {
        Block[] machines = machineBlocks();
        if (machines.length > 0) {
            event.register((stack, tintIndex) -> stack.getItem() instanceof BlockItem item
                    ? casingColor(item.getBlock(), tintIndex)
                    : -1, machines);
        }
    }

    private static Block[] machineBlocks() {
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof MachineBlock)
                .toArray(Block[]::new);
    }

    private static int casingColor(Block block, int tintIndex) {
        return tintIndex == 0 && block instanceof MachineBlock machine
                ? 0xFF000000 | machine.tier().casingColor()
                : -1;
    }

    private static int grassColor(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        return level != null && pos != null
                ? BiomeColors.getAverageGrassColor(level, pos)
                : GrassColor.getDefaultColor();
    }

    @SubscribeEvent
    public static void wrapCropModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) -> {
            if (isCropStick(location)) {
                return new CropPlantModel(model);
            }
            return model;
        });
    }

    private static boolean isCropStick(ModelResourceLocation location) {
        return Gt6Crops.MODID.equals(location.id().getNamespace())
                && (location.id().getPath().equals("crop_stick")
                        || location.id().getPath().equals("block/crop_stick")
                        || location.id().getPath().equals("block/cross_crop_stick"));
    }
}
