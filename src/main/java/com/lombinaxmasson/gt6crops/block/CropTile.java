package com.lombinaxmasson.gt6crops.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.card.CropMutations;
import com.lombinaxmasson.gt6crops.rules.CropBreeding;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Server-owned crop state. The block state stores only a small visual age;
 * species, genetics and environment storage are persisted here.
 */
public final class CropTile extends BlockEntity {
    public static final ModelProperty<String> CROP = new ModelProperty<>();
    public static final ModelProperty<Integer> SIZE = new ModelProperty<>();
    private static final int MAX_STORAGE = 32;

    private String cardId = "";
    private int size;
    private CropRules.Stats stats = new CropRules.Stats(0, 0, 0);
    private long growthProgress;
    private int nutrientStorage = 8;
    private int hydrationStorage = 8;
    private int weedExStorage;
    private CropRules.Environment environment = new CropRules.Environment(10, 10, 10);

    public CropTile(BlockPos pos, BlockState state) {
        super(Gt6Crops.CROP_TILE.get(), pos, state);
    }

    public String cardId() {
        return cardId;
    }

    public int size() {
        return size;
    }

    public CropRules.Stats stats() {
        return stats;
    }

    public CropRules.Environment environment() {
        return environment;
    }

    public boolean isEmpty() {
        return cardId.isEmpty();
    }

    public boolean isCrossTile() {
        BlockState state = getBlockState();
        return state.hasProperty(CropStickBlock.CROSS) && state.getValue(CropStickBlock.CROSS);
    }

    public boolean canUpgrade() {
        return isEmpty() && !isCrossTile();
    }

    public void upgradeToCross() {
        if (level == null || !canUpgrade()) {
            return;
        }
        level.setBlock(
                worldPosition,
                getBlockState().setValue(CropStickBlock.CROSS, true),
                Block.UPDATE_ALL);
        setChanged();
    }

    public void removeCross(Player player) {
        if (level == null || !isCrossTile() || !isEmpty()) {
            return;
        }
        level.setBlock(
                worldPosition,
                getBlockState().setValue(CropStickBlock.CROSS, false),
                Block.UPDATE_ALL);
        if (!player.getAbilities().instabuild) {
            give(player, new ItemStack(Gt6Crops.CROP_STICK_ITEM.get()));
        }
        setChanged();
    }

    public Optional<CropCard> card() {
        return cardId.isEmpty() ? Optional.empty() : CropCards.find(cardId);
    }

    public void plant(CropCard card, CropRules.Stats newStats) {
        cardId = card.id();
        size = 1;
        stats = newStats;
        growthProgress = 0;
        setChanged();
        syncAge();
    }

    public boolean applyWeedEx() {
        if (card().map(CropCard::isWeed).orElse(false)) {
            clearCrop();
        }
        weedExStorage = Math.min(MAX_STORAGE, weedExStorage + 8);
        setChanged();
        return true;
    }

    public boolean applyFertilizer() {
        nutrientStorage = Math.min(MAX_STORAGE, nutrientStorage + 8);
        setChanged();
        return true;
    }

    public boolean applyWater() {
        hydrationStorage = Math.min(MAX_STORAGE, hydrationStorage + 8);
        setChanged();
        return true;
    }

    public boolean harvest(Player player) {
        if (level == null || level.isClientSide || cardId.isEmpty()) {
            return false;
        }
        CropCard crop = card().orElse(null);
        if (crop == null || !crop.canHarvest(size)) {
            return false;
        }

        int amount = crop.isWeed()
                ? 0
                : CropRules.harvestCount(stats, new Random(level.random.nextLong()));
        crop.harvestDrop().ifPresent(drop -> {
            ItemStack harvested = drop.copy();
            harvested.setCount(Math.min(
                    harvested.getMaxStackSize(),
                    harvested.getCount() * amount));
            give(player, harvested);
        });
        for (ItemStack extra : crop.extraDrops(level.random)) {
            give(player, extra);
        }
        if (!crop.isWeed()) {
            give(player, CropSeedItem.create(crop, stats));
        }

        if (crop.afterHarvestSize() <= 0) {
            clearCrop();
        } else {
            size = crop.afterHarvestSize();
            growthProgress = 0;
            syncAge();
            setChanged();
        }
        return true;
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, CropTile crop) {
        long cycle = Math.floorMod(level.getGameTime() + pos.asLong(), 256L);
        if (cycle != 0L) {
            return;
        }
        crop.refreshEnvironment(level, pos);
        crop.weedExStorage = Math.max(0, crop.weedExStorage - 1);

        if (crop.isEmpty()) {
            if (crop.isCrossTile()) {
                crop.tryCrossbreed(level, pos);
            } else if (crop.weedExStorage == 0
                    && CropRules.shouldSpawnWeed(new Random(level.random.nextLong()))) {
                CropCards.weed().ifPresent(weed ->
                        crop.plant(weed, new CropRules.Stats(0, 0, 0)));
            }
            return;
        }

        CropCard card = crop.card().orElse(null);
        if (card == null) {
            crop.clearCrop();
            return;
        }
        Random random = new Random(level.random.nextLong());
        if (card.isWeed()) {
            crop.trySpreadWeed(level, pos, random);
            return;
        }

        if (card.canGrow(crop.size)) {
            int points = CropRules.growthPoints(
                    card,
                    crop.stats,
                    crop.environment,
                    crop.nutrientStorage > 0,
                    crop.hydrationStorage > 0);
            crop.growthProgress += points;
            if (crop.growthProgress >= CropRules.growthThreshold(card)) {
                crop.growthProgress = 0;
                crop.size++;
                crop.nutrientStorage = Math.max(0, crop.nutrientStorage - 1);
                crop.hydrationStorage = Math.max(0, crop.hydrationStorage - 1);
                crop.setChanged();
                crop.syncAge();
            }
        }
        if (crop.size >= 2
                && CropRules.shouldSpreadWeed(crop.stats.resistance(), random)) {
            crop.trySpreadWeed(level, pos, random);
        }
    }

    private void tryCrossbreed(Level level, BlockPos pos) {
        Random random = new Random(level.random.nextLong());
        if (!CropRules.shouldAttemptCrossbreed(random)) {
            return;
        }
        List<CropBreeding.Parent> parents = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!(level.getBlockEntity(pos.relative(direction)) instanceof CropTile other)) {
                continue;
            }
            CropCard card = other.card().orElse(null);
            if (card == null || card.isWeed()) {
                continue;
            }
            parents.add(new CropBreeding.Parent(
                    card.id(),
                    other.stats,
                    card.canCross(other.size),
                    card.canBreed(other.size),
                    other.nutrientStorage > 0));
        }
        CropBreeding.Outcome outcome = CropMutations.book().resolve(parents, random);
        if (outcome == null) {
            return;
        }
        CropCards.find(outcome.cardId()).ifPresent(card -> plant(card, outcome.stats()));
    }

    private void trySpreadWeed(Level level, BlockPos pos, Random random) {
        if (!CropRules.shouldSpreadWeed(stats.resistance(), random)
                || weedExStorage > 0) {
            return;
        }
        CropCard weed = CropCards.weed().orElse(null);
        if (weed == null) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof CropTile target
                    && target.isEmpty()) {
                target.plant(weed, new CropRules.Stats(0, 0, 0));
                return;
            }
        }
    }

    private void refreshEnvironment(Level level, BlockPos pos) {
        BlockState soil = level.getBlockState(pos.below());
        int nutrients = soil.is(Blocks.FARMLAND)
                ? 3 + soil.getValue(FarmBlock.MOISTURE)
                : 4;
        int humidity = soil.getFluidState().is(FluidTags.WATER) ? 10 : 4;
        for (BlockPos nearby : BlockPos.withinManhattan(pos, 2, 1, 2)) {
            if (level.getFluidState(nearby).is(FluidTags.WATER)) {
                humidity = 10;
                break;
            }
        }
        int air = level.canSeeSky(pos.above()) ? 10 : 5;
        environment = new CropRules.Environment(nutrients, humidity, air);
        setChanged();
    }

    private void clearCrop() {
        cardId = "";
        size = 0;
        stats = new CropRules.Stats(0, 0, 0);
        growthProgress = 0;
        syncAge();
        setChanged();
    }

    private void syncAge() {
        if (level == null) {
            return;
        }
        int age = cardId.isEmpty() ? 0 : Math.min(7, Math.max(1, size));
        BlockState oldState = getBlockState();
        if (oldState.getValue(CropStickBlock.AGE) != age) {
            level.setBlock(worldPosition, oldState.setValue(CropStickBlock.AGE, age), 3);
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        requestModelDataUpdate();
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(CROP, cardId).with(SIZE, size).build();
    }

    private static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Card", cardId);
        tag.putInt("Size", size);
        tag.putInt("Growth", stats.growth());
        tag.putInt("Gain", stats.gain());
        tag.putInt("Resistance", stats.resistance());
        tag.putLong("GrowthProgress", growthProgress);
        tag.putInt("NutrientStorage", nutrientStorage);
        tag.putInt("HydrationStorage", hydrationStorage);
        tag.putInt("WeedExStorage", weedExStorage);
        tag.putInt("Nutrients", environment.nutrients());
        tag.putInt("Humidity", environment.humidity());
        tag.putInt("AirQuality", environment.airQuality());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cardId = tag.getString("Card");
        size = Math.max(0, tag.getInt("Size"));
        stats = new CropRules.Stats(
                tag.getInt("Growth"),
                tag.getInt("Gain"),
                tag.getInt("Resistance"));
        growthProgress = Math.max(0, tag.getLong("GrowthProgress"));
        nutrientStorage = clampStorage(tag.getInt("NutrientStorage"));
        hydrationStorage = clampStorage(tag.getInt("HydrationStorage"));
        weedExStorage = clampStorage(tag.getInt("WeedExStorage"));
        environment = new CropRules.Environment(
                tag.getInt("Nutrients"),
                tag.getInt("Humidity"),
                tag.getInt("AirQuality"));
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static int clampStorage(int value) {
        return Math.max(0, Math.min(MAX_STORAGE, value));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
