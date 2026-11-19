package com.lombinaxmasson.gt6crops.gametest;

import java.util.List;
import java.util.Locale;

import gregtech.api.addon.platform.AddonItems;
import gregtech.api.addon.platform.AddonRecipes;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.block.CropStickBlock;
import com.lombinaxmasson.gt6crops.block.CropTile;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gt6crops")
@PrefixGameTestTemplate(false)
public final class MaterialCropTests {
    private static final List<MaterialCross> CROSSES = List.of(
            new MaterialCross("coppon", "Copper", "copper_fiber", "carrot", "orange_tulip"),
            new MaterialCross("tine", "Tin", "tin_twig", "pumpkin", "melon"),
            new MaterialCross("ferru", "Iron", "iron_leaf", "coppon", "tine"),
            new MaterialCross("galvania", "Zinc", "zinc_leaf", "tine", "ferru"),
            new MaterialCross("nickelback", "Nickel", "nickel_leaf", "ferru", "coppon"),
            new MaterialCross("bauxia", "Aluminium", "aluminium_leaf", "galvania", "nickelback"),
            new MaterialCross("plumbilia", "Lead", "lead_leaf", "coppon", "withereed"),
            new MaterialCross("argentia", "Silver", "silver_leaf", "plumbilia", "tine"),
            new MaterialCross("aurelia", "Gold", "gold_leaf", "plumbilia", "coppon"));

    private MaterialCropTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void metalCropsCrossAndTheirHarvestsProcess(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 0, 4)).atY(250);
        level.getChunk(center);
        for (MaterialCross cross : CROSSES) {
            matureParent(level, center.west(), cross.firstParent());
            matureParent(level, center.east(), cross.secondParent());
            CropTile crop = placeCropStick(level, center, Blocks.STONE, Blocks.DIRT, true);
            if (cross.child().equals("coppon")) {
                growUntilPlanted(level, center, crop, 64);
                helper.assertTrue(crop.isEmpty(), "copper needs a copper sample below the center bed");
            }

            level.setBlockAndUpdate(center.below(2), mineralBlock(cross.material()).defaultBlockState());
            ItemStack harvest = crossAndHarvest(helper, center, crop, cross.child(), cross.item());
            ItemStack dust = AddonItems.stack("dustTiny", cross.material());
            helper.assertTrue(!dust.isEmpty(), "GT registers tiny dust for " + cross.material());
            assertProcessing(helper, "gt.recipe.mortar", harvest, dust);
            assertProcessing(helper, "gt.recipe.shredder", harvest, dust);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void oilBerriesCrossAndPressIntoOil(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 0, 4)).atY(250);
        level.getChunk(center);
        matureParent(level, center.west(), "withereed");
        matureParent(level, center.east(), "nether_wart");

        CropTile crop = placeCropStick(level, center, Blocks.STONE, Blocks.DIRT, true);
        growUntilPlanted(level, center, crop, 64);
        helper.assertTrue(crop.isEmpty(), "oil berries need a suitable bed before a cross can take hold");

        level.setBlockAndUpdate(center.below(), Blocks.SAND.defaultBlockState());
        ItemStack harvest = crossAndHarvest(helper, center, crop, "oil_berries", "oil_berries");
        AddonRecipes.Line recipe = AddonRecipes.map("gt.recipe.squeezer")
                .find(List.of(harvest.copyWithCount(1)), List.of(), false, 2, 2);
        helper.assertTrue(recipe != null, "the squeezer accepts a single oil berry");
        FluidStack[] output = recipe.fluidOutputs();
        ResourceLocation oil = ResourceLocation.fromNamespaceAndPath("gt6ce", "oil");
        helper.assertTrue(output.length == 1 && output[0].getAmount() == 100
                        && oil.equals(BuiltInRegistries.FLUID.getKey(output[0].getFluid())),
                "the squeezer yields 100 mB of GT oil");
        helper.assertTrue(recipe.itemOutputs().length == 0, "oil pressing yields no item byproducts");
        helper.succeed();
    }

    private static ItemStack crossAndHarvest(
            GameTestHelper helper, BlockPos center, CropTile crop, String child, String item) {
        growUntilPlanted(helper.getLevel(), center, crop, 256);
        helper.assertTrue(crop.card().map(CropCard::id).filter(child::equals).isPresent(),
                child + " grows from its specified parents on the matching bed");
        helper.assertTrue(!crop.isScanned(), "a cross produces an unscanned crop");
        crop.setSize(card(child).maxSize());
        ResourceLocation dropId = ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, item);
        ItemStack harvest = crop.machineHarvest(1.0).stream()
                .filter(stack -> dropId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())))
                .findFirst()
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(!harvest.isEmpty(), child + " drops " + dropId);
        return harvest;
    }

    private static void growUntilPlanted(ServerLevel level, BlockPos center, CropTile crop, int maxCycles) {
        for (int cycle = 0; cycle < maxCycles && crop.isEmpty(); cycle++) {
            crop.runGrowthCycle(level, center);
        }
    }

    private static void assertProcessing(GameTestHelper helper, String map, ItemStack harvest, ItemStack dust) {
        AddonRecipes.Line recipe = AddonRecipes.map(map).find(List.of(harvest.copyWithCount(1)), List.of(), false, 2, 2);
        helper.assertTrue(recipe != null, map + " accepts " + harvest.getItem());
        ItemStack[] output = recipe.itemOutputs();
        helper.assertTrue(output.length == 1 && output[0].getCount() == 1
                        && ItemStack.isSameItemSameComponents(dust, output[0]),
                map + " processes " + harvest.getItem() + " into one tiny dust of its material");
    }

    private static void matureParent(ServerLevel level, BlockPos pos, String id) {
        CropCard card = card(id);
        MaterialCross material = CROSSES.stream().filter(cross -> cross.child().equals(id)).findFirst().orElse(null);
        Block soil = material != null ? Blocks.STONE : switch (id) {
            case "orange_tulip" -> Blocks.DIRT;
            case "withereed" -> Blocks.STONE;
            case "nether_wart" -> Blocks.SOUL_SAND;
            default -> Blocks.FARMLAND;
        };
        Block subsoil = Blocks.DIRT;
        if (material != null) {
            subsoil = mineralBlock(material.material());
        } else if (id.equals("withereed")) {
            subsoil = mineralBlock("Coal");
        }
        CropTile crop = placeCropStick(level, pos, soil, subsoil, false);
        crop.plant(card, new CropRules.Stats(1, 1, 1), true);
        crop.setSize(card.maxSize());
    }

    private static CropCard card(String id) {
        return CropCards.find(id).orElseThrow(() -> new IllegalStateException("Missing crop card " + id));
    }

    private static Block mineralBlock(String material) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                Gt6Crops.MODID, "sub_soil/" + material.toLowerCase(Locale.ROOT));
        return BuiltInRegistries.BLOCK.getTag(BlockTags.create(id))
                .flatMap(tag -> tag.stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("Missing mineral sample for " + material))
                .value();
    }

    private static CropTile placeCropStick(ServerLevel level, BlockPos pos, Block soil, Block subsoil, boolean cross) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(2), subsoil.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), soil.defaultBlockState());
        level.setBlockAndUpdate(pos, Gt6Crops.CROP_STICK.get().defaultBlockState().setValue(CropStickBlock.CROSS, cross));
        return (CropTile) level.getBlockEntity(pos);
    }

    private record MaterialCross(String child, String material, String item, String firstParent, String secondParent) {}
}
