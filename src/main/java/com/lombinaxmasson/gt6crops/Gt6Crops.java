package com.lombinaxmasson.gt6crops;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;

import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.addon.GregTechAddons;
import com.mojang.logging.LogUtils;
import com.lombinaxmasson.gt6crops.block.CropStickBlock;
import com.lombinaxmasson.gt6crops.block.CropTile;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.command.Gt6CropsCommands;
import com.lombinaxmasson.gt6crops.fluid.CropFluid;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.item.PlantCureItem;
import com.lombinaxmasson.gt6crops.item.WeedExItem;
import com.lombinaxmasson.gt6crops.machine.CropManagerBlock;
import com.lombinaxmasson.gt6crops.machine.CropManagerMenu;
import com.lombinaxmasson.gt6crops.machine.CropManagerTile;
import com.lombinaxmasson.gt6crops.machine.MachineBlock;
import com.lombinaxmasson.gt6crops.machine.MachineTier;
import com.lombinaxmasson.gt6crops.machine.MachineTile;
import com.lombinaxmasson.gt6crops.machine.SeedGeneratorBlock;
import com.lombinaxmasson.gt6crops.machine.SeedGeneratorMenu;
import com.lombinaxmasson.gt6crops.machine.SeedGeneratorTile;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * GT6 Crops: an IC2-inspired crop-stick and crossbreeding addon for GT6CE.
 *
 * <p>The addon entry point is registered in the constructor because GT6 closes
 * addon registration before common setup begins.</p>
 */
@Mod(Gt6Crops.MODID)
public final class Gt6Crops {
    public static final String MODID = "gt6crops";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, MODID);

    public static final CropFluid FERTILIZER =
            new CropFluid(FLUID_TYPES, FLUIDS, "fertilizer", 0x2DAA2D);
    public static final CropFluid ENRICHED_FERTILIZER =
            new CropFluid(FLUID_TYPES, FLUIDS, "enriched_fertilizer", 0x28E515);

    public static final DeferredBlock<Block> CROP_STICK =
            BLOCKS.register("crop_stick", CropStickBlock::new);

    public static final DeferredItem<BlockItem> CROP_STICK_ITEM =
            ITEMS.register("crop_stick", () -> new BlockItem(
                    CROP_STICK.get(), new Item.Properties()));
    public static final DeferredItem<CropSeedItem> CROP_SEED =
            ITEMS.register("crop_seed", () -> new CropSeedItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<WeedExItem> WEED_EX =
            ITEMS.register("weed_ex", () -> new WeedExItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<PlantCureItem> PLANT_CURE =
            ITEMS.register("plant_cure", () -> new PlantCureItem(new Item.Properties().durability(100)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CropTile>> CROP_TILE =
            BLOCK_ENTITIES.register("crop_tile", () -> BlockEntityType.Builder
                    .of(CropTile::new, CROP_STICK.get())
                    .build(null));

    public static final Map<MachineTier, DeferredBlock<CropManagerBlock>> CROP_MANAGERS =
            machines("crop_manager", CropManagerBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CropManagerTile>> CROP_MANAGER_TILE =
            BLOCK_ENTITIES.register("crop_manager", () -> BlockEntityType.Builder
                    .of(CropManagerTile::new, blocks(CROP_MANAGERS))
                    .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CropManagerMenu>> CROP_MANAGER_MENU =
            MENUS.register("crop_manager", () -> IMenuTypeExtension.create(CropManagerMenu::new));

    public static final Map<MachineTier, DeferredBlock<SeedGeneratorBlock>> SEED_GENERATORS =
            machines("seed_generator", SeedGeneratorBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeedGeneratorTile>> SEED_GENERATOR_TILE =
            BLOCK_ENTITIES.register("seed_generator", () -> BlockEntityType.Builder
                    .of(SeedGeneratorTile::new, blocks(SEED_GENERATORS))
                    .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SeedGeneratorMenu>> SEED_GENERATOR_MENU =
            MENUS.register("seed_generator", () -> IMenuTypeExtension.create(SeedGeneratorMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gt6crops"))
                    .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                    .icon(() -> seedIcon())
                    .displayItems((parameters, output) -> {
                        output.accept(CROP_STICK_ITEM.get());
                        output.accept(WEED_EX.get());
                        output.accept(PLANT_CURE.get());
                        CROP_MANAGERS.values().forEach(output::accept);
                        SEED_GENERATORS.values().forEach(output::accept);
                        if (!CropCards.initialized()) {
                            return;
                        }
                        CropRules.Stats creative = new CropRules.Stats(1, 1, 1);
                        for (CropCard card : CropCards.cards()) {
                            if (!card.isWeed()) {
                                output.accept(CropSeedItem.create(card, creative));
                            }
                        }
                    })
                    .build());

    public Gt6Crops(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENUS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        modEventBus.addListener(Gt6Crops::onRegisterCapabilities);
        modContainer.registerConfig(ModConfig.Type.COMMON, Gt6CropsConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(Gt6Crops::onRegisterCommands);

        GregTechAddons.register(new GregTechAddon() {
            @Override
            public String id() {
                return MODID;
            }

            @Override
            public void onRecipesReady(Context context) {
                CropCards.initialize();
                GtRecipeIntegration.register(context);
                FertilizerRecipes.register(context);
                LOGGER.info("gt6crops: {} crop cards available ({} {})",
                        CropCards.cards().size(), context.platform(), context.minecraftVersion());
            }
        });
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        Gt6CropsCommands.register(event.getDispatcher());
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        MachineTile.registerCapabilities(event, CROP_MANAGER_TILE.get());
        MachineTile.registerCapabilities(event, SEED_GENERATOR_TILE.get());
    }

    /** One block and block item per tier, named {@code <name>_<tier>}. */
    private static <B extends MachineBlock> Map<MachineTier, DeferredBlock<B>> machines(
            String name, Function<MachineTier, B> factory) {
        Map<MachineTier, DeferredBlock<B>> result = new EnumMap<>(MachineTier.class);
        for (MachineTier tier : MachineTier.values()) {
            DeferredBlock<B> block = BLOCKS.register(name + "_" + tier.id(), () -> factory.apply(tier));
            ITEMS.registerSimpleBlockItem(block);
            result.put(tier, block);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Block[] blocks(Map<MachineTier, ? extends DeferredBlock<? extends Block>> machines) {
        return machines.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
    }

    private static ItemStack seedIcon() {
        if (!CropCards.initialized()) {
            return new ItemStack(CROP_STICK_ITEM.get());
        }
        return CropCards.find("wheat")
                .map(card -> CropSeedItem.create(card, new CropRules.Stats(1, 1, 1)))
                .orElseGet(() -> new ItemStack(CROP_STICK_ITEM.get()));
    }
}
