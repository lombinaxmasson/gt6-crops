package com.lombinaxmasson.gt6crops;

import org.slf4j.Logger;

import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.addon.GregTechAddons;
import com.mojang.logging.LogUtils;
import com.lombinaxmasson.gt6crops.block.CropStickBlock;
import com.lombinaxmasson.gt6crops.block.CropTile;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.item.WeedExItem;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

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

    public static final DeferredBlock<Block> CROP_STICK =
            BLOCKS.register("crop_stick", CropStickBlock::new);

    public static final DeferredItem<BlockItem> CROP_STICK_ITEM =
            ITEMS.register("crop_stick", () -> new BlockItem(
                    CROP_STICK.get(), new Item.Properties()));
    public static final DeferredItem<CropSeedItem> CROP_SEED =
            ITEMS.register("crop_seed", () -> new CropSeedItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<WeedExItem> WEED_EX =
            ITEMS.register("weed_ex", () -> new WeedExItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CropTile>> CROP_TILE =
            BLOCK_ENTITIES.register("crop_tile", () -> BlockEntityType.Builder
                    .of(CropTile::new, CROP_STICK.get())
                    .build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gt6crops"))
                    .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                    .icon(() -> seedIcon())
                    .displayItems((parameters, output) -> {
                        output.accept(CROP_STICK_ITEM.get());
                        output.accept(WEED_EX.get());
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

        GregTechAddons.register(new GregTechAddon() {
            @Override
            public String id() {
                return MODID;
            }

            @Override
            public void onRecipesReady(Context context) {
                CropCards.initialize();
                GtRecipeIntegration.register(context);
                LOGGER.info("gt6crops: {} crop cards available ({} {})",
                        CropCards.cards().size(), context.platform(), context.minecraftVersion());
            }
        });
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
