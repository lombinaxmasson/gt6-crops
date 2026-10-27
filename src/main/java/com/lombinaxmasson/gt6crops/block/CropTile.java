package com.lombinaxmasson.gt6crops.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import gregtech.api.crop.CropScanSource;
import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.Gt6CropsConfig;
import com.lombinaxmasson.gt6crops.Gt6CropsTags;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.card.CropConditions;
import com.lombinaxmasson.gt6crops.card.CropMutations;
import com.lombinaxmasson.gt6crops.rules.CropBreeding;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
public final class CropTile extends BlockEntity implements CropScanSource {
    public static final ModelProperty<String> CROP = new ModelProperty<>();
    public static final ModelProperty<Integer> SIZE = new ModelProperty<>();
    public static final ModelProperty<Boolean> SICK = new ModelProperty<>();
    public static final int MAX_STORAGE = 32;
    private static final int HAND_DOSE = 8;
    private static final int FULLY_SCANNED = 4;
    /** CropsNH adds 11 of its 100 water per rainy cycle; this is the same share of 32. */
    private static final int RAIN_WATER = 4;
    private static final int WEED_EX_PER_BLOCKED_INFECTION = 2;
    private static final int WEED_EX_PER_BLOCKED_WEED = 5;
    /** CropsNH's Plant Cure tops fertilizer up to a quarter so the crop does not relapse at once. */
    private static final int CURE_FERTILIZER = MAX_STORAGE / 4;

    private String cardId = "";
    private int size;
    private CropRules.Stats stats = new CropRules.Stats(0, 0, 0);
    private long growthProgress;
    private int nutrientStorage = 8;
    private int hydrationStorage = 8;
    private int weedExStorage;
    private boolean sick;
    /** CropsNH: a crossbred crop hides its stats until a magnifying glass or crop scanner looks at it. */
    private boolean scanned = true;
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

    public int nutrientStorage() {
        return nutrientStorage;
    }

    public int hydrationStorage() {
        return hydrationStorage;
    }

    public int weedExStorage() {
        return weedExStorage;
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

    public void plant(CropCard card, CropRules.Stats newStats, boolean newScanned) {
        cardId = card.id();
        size = 1;
        stats = newStats;
        scanned = newScanned;
        growthProgress = 0;
        sick = false;
        setChanged();
        syncAge();
    }

    public boolean isSick() {
        return sick;
    }

    public boolean isScanned() {
        return scanned;
    }

    /** Marks the planted crop as scanned, so its seeds show their stats. */
    public void scan() {
        if (!isEmpty() && !scanned) {
            scanned = true;
            setChanged();
        }
    }

    /** Plant Cure heals a sick crop and gives it a little fertilizer. */
    public boolean cure() {
        if (!sick) {
            return false;
        }
        nutrientStorage = Math.max(nutrientStorage, CURE_FERTILIZER);
        setSick(false);
        return true;
    }

    /** The client tints a sick crop, so a change of health is synced. */
    private void setSick(boolean newSick) {
        if (sick == newSick) {
            return;
        }
        sick = newSick;
        setChanged();
        sync();
    }

    public boolean applyWeedEx() {
        if (card().map(CropCard::isWeed).orElse(false)) {
            clearCrop();
        }
        addWeedEx(HAND_DOSE);
        return true;
    }

    public boolean applyFertilizer() {
        addFertilizer(HAND_DOSE);
        return true;
    }

    public boolean applyWater() {
        addWater(HAND_DOSE);
        return true;
    }

    /** Adds water up to {@link #MAX_STORAGE} and returns how much went in. */
    public int addWater(int amount) {
        int added = room(hydrationStorage, amount);
        if (added > 0) {
            hydrationStorage += added;
            setChanged();
        }
        return added;
    }

    /** Adds fertilizer up to {@link #MAX_STORAGE} and returns how much went in. */
    public int addFertilizer(int amount) {
        int added = room(nutrientStorage, amount);
        if (added > 0) {
            nutrientStorage += added;
            setChanged();
        }
        return added;
    }

    /**
     * Adds Weed-EX up to {@link #MAX_STORAGE} and returns how much went in.
     * Unlike a hand spray it leaves a weed standing, as CropsNH's Crop Manager does.
     */
    public int addWeedEx(int amount) {
        int added = room(weedExStorage, amount);
        if (added > 0) {
            weedExStorage += added;
            setChanged();
        }
        return added;
    }

    private static int room(int stored, int amount) {
        return Math.max(0, Math.min(amount, MAX_STORAGE - stored));
    }

    public boolean harvest(Player player) {
        CropCard crop = ripeCrop();
        if (crop == null) {
            return false;
        }
        harvestDrops(crop, level.random).forEach(stack -> give(player, stack));
        cutBack(crop);
        return true;
    }

    /**
     * A machine harvest: a ripe crop's drops scaled by {@code multiplier}, or nothing
     * if the crop is not ripe. The crop is cut back as a hand harvest does.
     */
    public List<ItemStack> machineHarvest(double multiplier) {
        CropCard crop = ripeCrop();
        if (crop == null) {
            return List.of();
        }
        List<ItemStack> drops = harvestDrops(crop, level.random, multiplier);
        cutBack(crop);
        return drops;
    }

    private CropCard ripeCrop() {
        if (level == null || level.isClientSide || cardId.isEmpty()) {
            return null;
        }
        CropCard crop = card().orElse(null);
        return crop == null || crop.isWeed() || !crop.canHarvest(size) ? null : crop;
    }

    private void cutBack(CropCard crop) {
        if (crop.afterHarvestSize() <= 0) {
            clearCrop();
        } else {
            size = crop.afterHarvestSize();
            growthProgress = 0;
            syncAge();
            setChanged();
        }
    }

    /**
     * A spade clears weeds, with a grass tuft from fully grown ones, and digs any
     * other crop up with its seed and, if ripe, its harvest.
     */
    public boolean uproot(Player player) {
        if (level == null || level.isClientSide || cardId.isEmpty()) {
            return false;
        }
        CropCard crop = card().orElse(null);
        if (crop != null && crop.isWeed()) {
            if (size >= crop.maxSize()) {
                give(player, new ItemStack(Items.SHORT_GRASS));
            }
        } else if (crop != null) {
            if (crop.canHarvest(size)) {
                harvestDrops(crop, level.random).forEach(stack -> give(player, stack));
            }
            give(player, CropSeedItem.create(crop, stats, scanned));
        }
        clearCrop();
        return true;
    }

    /** What the crop adds to a broken stick's drops. Does not change the crop. */
    public List<ItemStack> removalDrops(RandomSource random) {
        CropCard crop = card().orElse(null);
        if (crop == null || crop.isWeed()) {
            return List.of();
        }
        List<ItemStack> drops = crop.canHarvest(size) ? harvestDrops(crop, random) : new ArrayList<>();
        if (CropRules.resists(stats.resistance(), new Random(random.nextLong()))) {
            drops.add(CropSeedItem.create(crop, stats, scanned));
        }
        return drops;
    }

    private List<ItemStack> harvestDrops(CropCard crop, RandomSource random) {
        return harvestDrops(crop, random, 1.0);
    }

    private List<ItemStack> harvestDrops(CropCard crop, RandomSource random, double multiplier) {
        List<ItemStack> drops = new ArrayList<>();
        int amount = CropRules.harvestCount(stats, multiplier, new Random(random.nextLong()));
        crop.harvestDrop().ifPresent(drop -> {
            ItemStack harvested = drop.copy();
            harvested.setCount(Math.min(
                    harvested.getMaxStackSize(),
                    harvested.getCount() * amount));
            drops.add(harvested);
        });
        drops.addAll(crop.extraDrops(random));
        drops.removeIf(ItemStack::isEmpty);
        return drops;
    }

    /** Chat lines shown by the GT magnifying glass, which scans the crop as CropsNH's plant lens does. */
    public List<Component> inspect() {
        scan();
        refreshEnvironmentNow();
        List<Component> lines = new ArrayList<>();
        CropCard crop = card().orElse(null);
        if (crop == null) {
            lines.add(Component.translatable(isCrossTile()
                    ? "message.gt6crops.inspect.cross"
                    : "message.gt6crops.inspect.empty"));
        } else {
            lines.add(Component.translatable(
                    "message.gt6crops.inspect.crop", crop.name(), size, crop.maxSize()));
            if (!crop.isWeed()) {
                lines.add(Component.translatable(
                        "message.gt6crops.inspect.stats",
                        stats.growth(), stats.gain(), stats.resistance()));
                inspectConditions(crop.conditions(), lines);
                inspectHealth(crop, lines);
            }
        }
        lines.add(Component.translatable(
                "message.gt6crops.inspect.storage",
                nutrientStorage, hydrationStorage, weedExStorage, MAX_STORAGE));
        lines.add(Component.translatable(
                "message.gt6crops.inspect.environment",
                environment.nutrients(),
                environment.humidity(),
                environment.airQuality(),
                CropRules.MAX_ENVIRONMENT));
        return lines;
    }

    private void inspectConditions(CropConditions conditions, List<Component> lines) {
        if (level == null) {
            return;
        }
        lines.addAll(GrowthCheck.describe(
                conditions,
                GrowthCheck.problems(conditions, level, worldPosition),
                level,
                worldPosition));
        if (!conditions.likedBiomes().isEmpty()) {
            lines.add(Component.translatable(
                    "message.gt6crops.inspect.liked_biomes",
                    GrowthCheck.join(conditions.likedBiomes()),
                    GrowthCheck.likedBiomes(conditions, level, worldPosition)));
        }
    }

    private void inspectHealth(CropCard crop, List<Component> lines) {
        if (sick) {
            lines.add(Component.translatable("message.gt6crops.inspect.sick"));
            return;
        }
        if (level == null || !crop.canGrow(size)) {
            return;
        }
        int score = nutrientScore(level, worldPosition, crop);
        if (CropRules.isStarving(crop.tier(), stats.growth(), score)) {
            lines.add(Component.translatable(
                    "message.gt6crops.inspect.starving",
                    score,
                    CropRules.nutrientsNeeded(crop.tier(), stats.growth())));
        }
    }

    private int nutrientScore(Level level, BlockPos pos, CropCard crop) {
        return CropRules.nutrientScore(
                hydrationStorage,
                nutrientStorage,
                MAX_STORAGE,
                level.canSeeSky(pos.above()),
                GrowthCheck.likedBiomes(crop.conditions(), level, pos),
                level.getBiome(pos).value().getModifiedClimateSettings().downfall());
    }

    @Override
    public CropScanData cropScanData() {
        CropCard crop = card().orElse(null);
        if (crop == null) {
            return null;
        }
        refreshEnvironmentNow();
        return new CropScanData(
                crop.name(),
                crop.attributes(),
                crop.discoveredBy(),
                stats.growth(),
                stats.gain(),
                stats.resistance(),
                nutrientStorage,
                hydrationStorage,
                weedExStorage,
                environment.nutrients(),
                environment.humidity(),
                environment.airQuality(),
                scanned ? FULLY_SCANNED : 0);
    }

    @Override
    public void setCropScanLevel(int scanLevel) {
        if (scanLevel >= FULLY_SCANNED) {
            scan();
        }
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, CropTile crop) {
        long cycle = Math.floorMod(level.getGameTime() + pos.asLong(), 256L);
        if (cycle != 0L) {
            return;
        }
        crop.runGrowthCycle(level, pos);
    }

    /**
     * One CropsNH growth cycle. Debug commands call this to skip the 256-tick wait.
     * Returns how many size stages the crop gained.
     */
    public int runGrowthCycle(Level level, BlockPos pos) {
        int before = size;
        refreshEnvironment(level, pos);
        if (level.isRainingAt(pos.above())) {
            hydrationStorage = Math.min(MAX_STORAGE, hydrationStorage + RAIN_WATER);
        }

        if (isEmpty()) {
            if (isCrossTile()) {
                tryCrossbreed(level, pos);
            } else if (Gt6CropsConfig.weedsEnabled()
                    && CropRules.shouldSpawnWeed(
                            Gt6CropsConfig.weedSpawnChance(), new Random(level.random.nextLong()))) {
                spawnWeed();
            }
            return size - before;
        }

        CropCard card = card().orElse(null);
        if (card == null) {
            clearCrop();
            return 0;
        }
        Random random = new Random(level.random.nextLong());
        if (sick) {
            spreadDisease(level, pos, random);
        } else if (card.canGrow(size)
                && (card.isWeed() || GrowthCheck.problems(card.conditions(), level, pos).isEmpty())) {
            grow(level, pos, card, random);
        }
        if (Gt6CropsConfig.weedsEnabled()
                && CropRules.spreadsWeeds(card, size, stats)
                && CropRules.shouldSpreadWeed(stats.growth(), Gt6CropsConfig.weedSpreadChance(), random)) {
            spreadWeed(level, pos, random);
        }
        return size - before;
    }

    /** Debug helper: run several growth cycles in a row. */
    public int forceGrowthCycles(int count) {
        if (level == null || level.isClientSide || count <= 0) {
            return 0;
        }
        int gained = 0;
        for (int i = 0; i < count; i++) {
            gained += runGrowthCycle(level, worldPosition);
        }
        return gained;
    }

    /** Debug helper: jump the planted crop to a size stage. */
    public boolean setSize(int newSize) {
        CropCard crop = card().orElse(null);
        if (crop == null) {
            return false;
        }
        size = Math.max(1, Math.min(crop.maxSize(), newSize));
        growthProgress = 0;
        setChanged();
        syncAge();
        return true;
    }

    /** Debug helper: rewrite the planted crop's genetics. */
    public boolean setStats(CropRules.Stats newStats) {
        if (isEmpty()) {
            return false;
        }
        stats = newStats;
        setChanged();
        return true;
    }

    /** Debug helper: top up water, fertilizer and Weed-EX. */
    public void fillStorage() {
        nutrientStorage = MAX_STORAGE;
        hydrationStorage = MAX_STORAGE;
        weedExStorage = MAX_STORAGE;
        setChanged();
    }

    /** A starving crop does not grow, and falls sick unless it passes a resistance check. */
    private void grow(Level level, BlockPos pos, CropCard card, Random random) {
        if (CropRules.isStarving(card.tier(), stats.growth(), nutrientScore(level, pos, card))) {
            if (!CropRules.resists(stats.resistance(), random)) {
                setSick(true);
            }
            return;
        }
        int points = CropRules.growthPoints(
                card,
                stats,
                environment,
                nutrientStorage > 0,
                hydrationStorage > 0,
                GrowthCheck.likedBiomes(card.conditions(), level, pos));
        growthProgress += (long) (points * Gt6CropsConfig.growthMultiplier());
        if (growthProgress >= CropRules.growthThreshold(card)) {
            growthProgress = 0;
            size++;
            nutrientStorage = Math.max(0, nutrientStorage - 1);
            hydrationStorage = Math.max(0, hydrationStorage - 1);
            setChanged();
            syncAge();
        }
    }

    private void tryCrossbreed(Level level, BlockPos pos) {
        Random random = new Random(level.random.nextLong());
        if (!CropRules.shouldAttemptCrossbreed(Gt6CropsConfig.breedingChance(), random)) {
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
        CropBreeding.Outcome outcome = CropMutations.book().resolve(
                parents, Gt6CropsConfig.breedingVariation(), random);
        if (outcome == null) {
            return;
        }
        CropCards.find(outcome.cardId())
                .filter(card -> GrowthCheck.problems(card.conditions(), level, pos).isEmpty())
                .ifPresent(card -> plant(card, outcome.stats(), false));
    }

    /** CropsNH: each cycle a sick crop exposes one random neighbouring crop. */
    private void spreadDisease(Level level, BlockPos pos, Random random) {
        List<CropTile> neighbours = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof CropTile other
                    && !other.isEmpty()) {
                neighbours.add(other);
            }
        }
        if (!neighbours.isEmpty()) {
            neighbours.get(random.nextInt(neighbours.size())).catchDisease(random);
        }
    }

    /** Resistance can shrug the disease off; otherwise stored Weed-EX is spent to block it. */
    private void catchDisease(Random random) {
        CropCard crop = card().orElse(null);
        if (crop == null || crop.isWeed() || sick || CropRules.resists(stats.resistance(), random)) {
            return;
        }
        if (weedExStorage > 0) {
            weedExStorage = Math.max(0, weedExStorage - WEED_EX_PER_BLOCKED_INFECTION);
            setChanged();
        } else {
            setSick(true);
        }
    }

    /**
     * CropsNH: weeds reach one random side. An empty stick takes a weed, a planted
     * crop is exposed to disease, and air over grass, dirt or farmland grows grass.
     */
    private void spreadWeed(Level level, BlockPos pos, Random random) {
        BlockPos target = pos.relative(Direction.from2DDataValue(random.nextInt(4)));
        if (level.getBlockEntity(target) instanceof CropTile other) {
            if (other.isEmpty()) {
                other.spawnWeed();
            } else {
                other.catchDisease(random);
            }
        } else if (level.isEmptyBlock(target)) {
            BlockPos ground = target.below();
            BlockState soil = level.getBlockState(ground);
            if (soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.FARMLAND)) {
                level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(target, Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Stored Weed-EX keeps the weed out, and is spent doing it. */
    private void spawnWeed() {
        if (weedExStorage > 0) {
            weedExStorage = Math.max(0, weedExStorage - WEED_EX_PER_BLOCKED_WEED);
            setChanged();
            return;
        }
        CropCards.weed().ifPresent(weed -> plant(weed, CropRules.WEED_STATS, true));
    }

    /** CropsNH: sprinting through a planted crop can trample it. */
    public void sprintedThrough(Entity entity) {
        CropCard crop = trampleable();
        if (crop != null && CropRules.tramplesWhenSprinting(
                crop.tier(), stats.resistance(), new Random(level.random.nextLong()))) {
            trample(entity);
        }
    }

    /** CropsNH: landing on a planted crop from more than 0.75 blocks can trample it. */
    public void landedOn(Entity entity, float fallDistance) {
        if (trampleable() != null
                && CropRules.tramplesWhenLanding(fallDistance, new Random(level.random.nextLong()))) {
            trample(entity);
        }
    }

    /** Weeds, empty sticks, resistance 31, and trample-proof soil are safe. */
    private CropCard trampleable() {
        if (level == null || level.isClientSide || !CropRules.canBeTrampled(stats.resistance())) {
            return null;
        }
        CropCard crop = card().orElse(null);
        if (crop == null || crop.isWeed()
                || level.getBlockState(worldPosition.below()).is(Gt6CropsTags.TRAMPLE_PROOF_SOIL)) {
            return null;
        }
        return crop;
    }

    /** The sticks break with their usual drops, and farmland under them turns to dirt. */
    private void trample(Entity entity) {
        BlockPos below = worldPosition.below();
        level.playSound(null, worldPosition, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.destroyBlock(worldPosition, true, entity);
        BlockState soil = level.getBlockState(below);
        if (soil.is(Blocks.FARMLAND)) {
            FarmBlock.turnToDirt(entity, soil, level, below);
        }
    }

    private void refreshEnvironmentNow() {
        if (level != null && !level.isClientSide) {
            refreshEnvironment(level, worldPosition);
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
        sick = false;
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
        sync();
    }

    /** Sends the tile to clients, which redraw it with the new model data. */
    private void sync() {
        if (level == null) {
            return;
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        requestModelDataUpdate();
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(CROP, cardId).with(SIZE, size).with(SICK, sick).build();
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
        tag.putBoolean("Sick", sick);
        tag.putBoolean("Scanned", scanned);
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
        sick = tag.getBoolean("Sick");
        // Crops planted before scanning existed have no flag and keep showing their stats.
        scanned = !tag.contains("Scanned") || tag.getBoolean("Scanned");
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
