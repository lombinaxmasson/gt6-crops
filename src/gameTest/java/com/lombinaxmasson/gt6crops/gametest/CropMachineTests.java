package com.lombinaxmasson.gt6crops.gametest;

import com.gregtech.gregtech.api.addon.platform.EnergyBlock;
import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.block.CropTile;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.machine.CropManagerBehavior;
import com.lombinaxmasson.gt6crops.machine.MachineSaveMigration;
import com.lombinaxmasson.gt6crops.machine.MachineTier;
import com.lombinaxmasson.gt6crops.machine.SeedGeneratorRules;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Crop Manager and Seed Generator on the behavior shell, including an old save. */
@GameTestHolder("gt6crops")
@PrefixGameTestTemplate(false)
public final class CropMachineTests {
    private CropMachineTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void cropManagerHarvestsARipeCrop(GameTestHelper helper) {
        CropCard card = harvestable();
        BlockPos machine = place(helper, "crop_manager_lv", new BlockPos(2, 0, 2));
        BlockPos crop = machine.east();
        CropTile planted = plant(helper, crop, card);
        planted.setSize(card.maxSize());
        int before = planted.size();
        IFluidHandler fluids = fluids(helper, machine);
        int enriched = fluids.fill(new FluidStack(Gt6Crops.ENRICHED_FERTILIZER.source(), 10), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(enriched == 10, "enriched fertilizer is accepted as a concentrate");
        helper.assertTrue(fluids.getFluidInTank(CropManagerBehavior.FERTILIZER_TANK).is(Gt6Crops.FERTILIZER.source())
                        && fluids.getFluidInTank(CropManagerBehavior.FERTILIZER_TANK).getAmount() == 100,
                "ten millibuckets of enriched fertilizer become one hundred of fertilizer");
        helper.assertTrue("gt6crops:crop_manager".equals(String.valueOf(
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(helper.getLevel().getBlockEntity(machine).getType()))),
                "the crop manager keeps its block entity id");
        for (int tick = 0; tick < 50; tick++) {
            power(helper, machine, 32);
        }
        helper.assertTrue(flag(helper.getLevel().getBlockState(machine), "lit"), "a powered manager lights up");
        helper.assertTrue(stillPlanted(helper, crop, planted), "the crop stick stays on its soil");
        if (card.afterHarvestSize() <= 0) {
            helper.assertTrue(planted.isEmpty(), "the ripe crop was cleared");
        } else {
            helper.assertTrue(planted.size() == card.afterHarvestSize() && planted.size() != before,
                    "the ripe crop was cut back");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void cropManagerWatersWhenTheSwitchIsOn(GameTestHelper helper) {
        CropCard card = harvestable();
        BlockPos machine = place(helper, "crop_manager_lv", new BlockPos(4, 0, 2));
        CropTile planted = plant(helper, machine.east(), card);
        int dry = planted.hydrationStorage();
        IFluidHandler fluids = fluids(helper, machine);
        helper.assertTrue(fluids.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "the water tank takes water");
        for (int tick = 0; tick < 50; tick++) {
            power(helper, machine, 32);
        }
        helper.assertTrue(planted.hydrationStorage() == dry, "watering stays off until the switch is flipped");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockEntity entity = helper.getLevel().getBlockEntity(machine);
        AbstractContainerMenu menu = ((MenuProvider) entity).createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu != null && menu.clickMenuButton(player, CropManagerBehavior.WATER),
                "the menu button turns watering on");
        menu.removed(player);
        for (int tick = 0; tick < 50; tick++) {
            power(helper, machine, 32);
        }
        helper.assertTrue(stillPlanted(helper, machine.east(), planted), "the crop stick stays on its soil");
        helper.assertTrue(planted.hydrationStorage() > dry, "watering fills the crop");
        helper.assertTrue(fluids.getFluidInTank(CropManagerBehavior.WATER_TANK).getAmount() < 1000,
                "watering spends water");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void cropManagerFertilizesWhenTheSwitchIsOn(GameTestHelper helper) {
        CropCard card = harvestable();
        BlockPos machine = place(helper, "crop_manager_lv", new BlockPos(6, 0, 2));
        CropTile planted = plant(helper, machine.east(), card);
        int hungry = planted.nutrientStorage();
        IFluidHandler fluids = fluids(helper, machine);
        helper.assertTrue(fluids.fill(new FluidStack(Gt6Crops.FERTILIZER.source(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "the fertilizer tank takes fertilizer");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AbstractContainerMenu menu = ((MenuProvider) helper.getLevel().getBlockEntity(machine))
                .createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu != null && menu.clickMenuButton(player, CropManagerBehavior.FERTILIZER),
                "the menu button turns fertilizing on");
        menu.removed(player);
        for (int tick = 0; tick < 50; tick++) {
            power(helper, machine, 32);
        }
        helper.assertTrue(stillPlanted(helper, machine.east(), planted), "the crop stick stays on its soil");
        helper.assertTrue(planted.nutrientStorage() > hungry, "fertilizing fills the crop");
        helper.assertTrue(fluids.getFluidInTank(CropManagerBehavior.FERTILIZER_TANK).getAmount() < 1000,
                "fertilizing spends fertilizer");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void cropManagerSpraysWeedExWhenTheSwitchIsOn(GameTestHelper helper) {
        CropCard card = harvestable();
        BlockPos machine = place(helper, "crop_manager_lv", new BlockPos(8, 0, 2));
        CropTile planted = plant(helper, machine.east(), card);
        int clean = planted.weedExStorage();
        Fluid poison = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("gregtech", "potion_poison_strong"));
        helper.assertTrue(poison != null && poison != Fluids.EMPTY, "strong poison is registered");
        IFluidHandler fluids = fluids(helper, machine);
        helper.assertTrue(fluids.fill(new FluidStack(poison, 100), IFluidHandler.FluidAction.EXECUTE) == 100,
                "the weed-ex tank takes strong poison");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AbstractContainerMenu menu = ((MenuProvider) helper.getLevel().getBlockEntity(machine))
                .createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu != null && menu.clickMenuButton(player, CropManagerBehavior.WEED_EX),
                "the menu button turns weed-ex on");
        menu.removed(player);
        for (int tick = 0; tick < 50; tick++) {
            power(helper, machine, 32);
        }
        helper.assertTrue(stillPlanted(helper, machine.east(), planted), "the crop stick stays on its soil");
        helper.assertTrue(planted.weedExStorage() > clean, "weed-ex coats the crop");
        helper.assertTrue(fluids.getFluidInTank(CropManagerBehavior.WEED_EX_TANK).getAmount() < 100,
                "spraying spends strong poison");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 500)
    public static void seedGeneratorCopiesAScannedSeed(GameTestHelper helper) {
        CropCard card = copyable();
        BlockPos machine = place(helper, "seed_generator_lv", new BlockPos(2, 0, 4));
        ItemStack seed = CropSeedItem.create(card, new CropRules.Stats(1, 1, 1), true);
        IItemHandler items = items(helper, machine);
        helper.assertTrue(items.insertItem(0, seed.copy(), false).isEmpty(), "the seed goes into the input");
        int cost = SeedGeneratorRules.fertilizerCost(new CropRules.Stats(1, 1, 1));
        helper.assertTrue(cost == 108, "stats 1, 1, 1 cost 108 mB");
        IFluidHandler fluids = fluids(helper, machine);
        helper.assertTrue(fluids.fill(new FluidStack(Gt6Crops.ENRICHED_FERTILIZER.source(), cost),
                IFluidHandler.FluidAction.EXECUTE) == cost, "the tank takes the enriched fertilizer");
        helper.assertTrue("gt6crops:seed_generator".equals(String.valueOf(
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(helper.getLevel().getBlockEntity(machine).getType()))),
                "the seed generator keeps its block entity id");
        for (int tick = 0; tick < 150; tick++) {
            power(helper, machine, 32);
        }
        helper.assertTrue(items.getStackInSlot(0).getCount() == 1, "the original seed stays");
        ItemStack copy = items.getStackInSlot(2);
        var data = CropSeedItem.data(copy);
        helper.assertTrue(data.isPresent() && data.get().scanned() && data.get().cardId().equals(card.id())
                        && data.get().stats().growth() == 1
                        && data.get().stats().gain() == 1
                        && data.get().stats().resistance() == 1
                        && copy.getCount() == 1,
                "the output is one copy of the scanned seed");
        helper.assertTrue(fluids.getFluidInTank(0).getAmount() == 0, "the copy spent its fertilizer");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void anOldCropManagerSaveLoads(GameTestHelper helper) {
        CropCard card = harvestable();
        BlockPos machine = place(helper, "crop_manager_lv", new BlockPos(2, 0, 6));
        CropTile planted = plant(helper, machine.east(), card);
        planted.setSize(card.maxSize());
        int hydration = planted.hydrationStorage();
        var lookup = helper.getLevel().registryAccess();
        ItemStackHandler inventory = new ItemStackHandler(CropManagerBehavior.SLOTS);
        inventory.setStackInSlot(CropManagerBehavior.FERTILIZER_START, new ItemStack(Items.BONE_MEAL, 3));
        FluidTank water = new FluidTank(CropManagerBehavior.waterCapacity(MachineTier.LV));
        water.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        ListTag tanks = new ListTag();
        tanks.add(water.writeToNBT(lookup, new CompoundTag()));
        tanks.add(new CompoundTag());
        tanks.add(new CompoundTag());
        ListTag overflow = new ListTag();
        overflow.add(new ItemStack(Items.STICK).save(lookup));
        CompoundTag saved = new CompoundTag();
        saved.putString("id", "gt6crops:crop_manager");
        saved.putInt("x", machine.getX());
        saved.putInt("y", machine.getY());
        saved.putInt("z", machine.getZ());
        saved.putLong("Energy", 32);
        saved.put("Items", inventory.serializeNBT(lookup));
        saved.put("Tanks", tanks);
        saved.put("Overflow", overflow);
        helper.assertTrue(MachineSaveMigration.migrate(saved), "the old tag is rewritten");
        helper.assertTrue(!saved.contains("Energy") && saved.getInt("gt.toggles") == 1,
                "a missing harvest switch stays on");
        BlockEntity entity = helper.getLevel().getBlockEntity(machine);
        entity.loadWithComponents(saved, lookup);
        IItemHandler items = items(helper, machine);
        IFluidHandler fluids = fluids(helper, machine);
        helper.assertTrue(items.getStackInSlot(CropManagerBehavior.FERTILIZER_START).is(Items.BONE_MEAL)
                        && items.getStackInSlot(CropManagerBehavior.FERTILIZER_START).getCount() == 3,
                "fertilizer items survive the old save");
        helper.assertTrue(fluids.getFluidInTank(0).is(Fluids.WATER)
                        && fluids.getFluidInTank(0).getAmount() == 1000,
                "water survives the old save");
        power(helper, machine, 0);
        helper.assertTrue(flag(helper.getLevel().getBlockState(machine), "lit"),
                "saved energy runs the first tick");
        for (int tick = 0; tick < 49; tick++) {
            power(helper, machine, 32);
        }
        if (card.afterHarvestSize() <= 0) {
            helper.assertTrue(planted.isEmpty(), "harvesting from the old save still cuts the crop");
        } else {
            helper.assertTrue(planted.size() == card.afterHarvestSize(),
                    "harvesting from the old save still cuts the crop");
        }
        helper.assertTrue(stillPlanted(helper, machine.east(), planted), "the crop stick stays on its soil");
        helper.assertTrue(planted.hydrationStorage() == hydration, "the old water switch stays off");
        helper.assertTrue(contains(items, Items.STICK), "overflow from the old save is dropped into the output");
        CompoundTag roundTrip = entity.saveWithoutMetadata(lookup);
        BlockPos other = place(helper, "crop_manager_lv", new BlockPos(4, 0, 6));
        helper.getLevel().getBlockEntity(other).loadWithComponents(roundTrip, lookup);
        helper.assertTrue(items(helper, other).getStackInSlot(CropManagerBehavior.FERTILIZER_START).is(Items.BONE_MEAL),
                "a shell save loads back into another crop manager");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void anOldActiveBlockStateStillParses(GameTestHelper helper) {
        CompoundTag legacy = stateTag("gt6crops:crop_manager_lv", "facing", "east", "active", "true");
        var parsed = BlockState.CODEC.parse(NbtOps.INSTANCE, legacy);
        helper.assertTrue(parsed.result().isPresent(),
                "legacy active state: " + parsed.error().map(Object::toString).orElse("no detail"));
        BlockState state = parsed.result().orElseThrow();
        helper.assertTrue(state.getBlock() == BuiltInRegistries.BLOCK.get(
                        ResourceLocation.fromNamespaceAndPath("gt6crops", "crop_manager_lv")),
                "the legacy name still selects the crop manager");
        helper.assertTrue(!flag(state, "lit") && !flag(state, "running"),
                "a missing lit and running default to off");
        CompoundTag current = stateTag("gt6crops:seed_generator_lv", "facing", "west", "lit", "true", "running", "true");
        var modern = BlockState.CODEC.parse(NbtOps.INSTANCE, current);
        helper.assertTrue(modern.result().isPresent() && flag(modern.result().orElseThrow(), "lit"),
                "the shell properties parse");
        helper.succeed();
    }

    private static CropCard harvestable() {
        for (CropCard card : CropCards.cards()) {
            if (!card.isWeed() && card.canHarvest(card.maxSize()) && card.afterHarvestSize() != card.maxSize()) {
                return card;
            }
        }
        throw new IllegalStateException("no harvestable crop");
    }

    private static CropCard copyable() {
        for (CropCard card : CropCards.cards()) {
            if (!card.isWeed() && card.duplicationCatalysts().isEmpty() && card.canDuplicate()) {
                return card;
            }
        }
        throw new IllegalStateException("no catalyst-free crop");
    }

    private static BlockPos place(GameTestHelper helper, String path, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative).atY(250);
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, path));
        helper.getLevel().getChunk(pos);
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof EnergyBlock,
                path + " placed as an energy block");
        return pos;
    }

    private static CropTile plant(GameTestHelper helper, BlockPos pos, CropCard card) {
        helper.getLevel().getChunk(pos);
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, Gt6Crops.CROP_STICK.get().defaultBlockState());
        CropTile tile = (CropTile) helper.getLevel().getBlockEntity(pos);
        tile.plant(card, new CropRules.Stats(1, 1, 1), true);
        helper.assertTrue(stillPlanted(helper, pos, tile), "the crop stick stays on its soil");
        return tile;
    }

    private static boolean stillPlanted(GameTestHelper helper, BlockPos pos, CropTile planted) {
        return helper.getLevel().getBlockState(pos).is(Gt6Crops.CROP_STICK.get())
                && helper.getLevel().getBlockEntity(pos) == planted;
    }

    /** {@code packet == 0} ticks whatever energy is already stored. */
    private static void power(GameTestHelper helper, BlockPos pos, long packet) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        BlockState state = helper.getLevel().getBlockState(pos);
        if (packet > 0) {
            long accepted = ((EnergyBlock) entity).doEnergyInjection("EU", null, packet, 1, true);
            helper.assertTrue(accepted == 1, "the machine accepts one " + packet + " EU packet");
        }
        @SuppressWarnings("unchecked")
        BlockEntityTicker<BlockEntity> ticker =
                (BlockEntityTicker<BlockEntity>) state.getTicker(helper.getLevel(), entity.getType());
        helper.assertTrue(ticker != null, "the machine ticks");
        ticker.tick(helper.getLevel(), pos, state, entity);
    }

    private static IItemHandler items(GameTestHelper helper, BlockPos pos) {
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        helper.assertTrue(handler != null, "item capability");
        return handler;
    }

    private static IFluidHandler fluids(GameTestHelper helper, BlockPos pos) {
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        helper.assertTrue(handler != null, "fluid capability");
        return handler;
    }

    private static boolean contains(IItemHandler items, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (items.getStackInSlot(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean flag(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name) && property instanceof BooleanProperty flag) {
                return state.getValue(flag);
            }
        }
        return false;
    }

    private static CompoundTag stateTag(String name, String... pairs) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        CompoundTag properties = new CompoundTag();
        for (int i = 0; i < pairs.length; i += 2) {
            properties.putString(pairs[i], pairs[i + 1]);
        }
        tag.put("Properties", properties);
        return tag;
    }
}
