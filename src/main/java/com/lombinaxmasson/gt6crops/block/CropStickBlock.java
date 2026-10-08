package com.lombinaxmasson.gt6crops.block;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.Gt6CropsTags;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.item.WeedExItem;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.sounds.SoundSource;

/** One crop-stick block. An empty stick accepts a second stick and becomes a cross. */
public final class CropStickBlock extends Block implements EntityBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
    public static final BooleanProperty CROSS = BooleanProperty.create("cross");
    private static final VoxelShape STICKS = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);
    private static final VoxelShape CROSS_STICKS = Block.box(0.0, 0.0, 0.0, 16.0, 13.0, 16.0);

    public CropStickBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .instabreak()
                .sound(SoundType.WOOD)
                .noOcclusion()
                .noCollission()
                .pushReaction(PushReaction.DESTROY));
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(CROSS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, CROSS);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(CROSS) ? CROSS_STICKS : STICKS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (state.getValue(CROSS)) {
            drops.add(new ItemStack(this));
        }
        return drops;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.FARMLAND);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CropTile(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != Gt6Crops.CROP_TILE.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) ->
                CropTile.serverTick(lvl, pos, blockState, (CropTile) blockEntity);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            net.minecraft.world.InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CropTile crop)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (GTToolHelper.isMagnifyingGlass(stack)) {
            if (!level.isClientSide) {
                crop.inspect().forEach(line -> player.displayClientMessage(line, false));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(Gt6Crops.CROP_STICK_ITEM.get()) && crop.canUpgrade()) {
            if (!level.isClientSide) {
                crop.upgradeToCross();
                consumeOne(stack, player);
                level.playSound(
                        null,
                        pos,
                        SoundType.WOOD.getPlaceSound(),
                        SoundSource.BLOCKS,
                        0.5F,
                        0.8F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.getItem() instanceof WeedExItem) {
            if (!level.isClientSide && crop.applyWeedEx()) {
                consumeOne(stack, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        ItemInteractionResult planted = offerSeed(level, crop, stack, player);
        if (planted.consumesAction()) {
            return planted;
        }

        if (stack.is(Gt6CropsTags.FERTILIZER)) {
            if (!level.isClientSide && crop.applyFertilizer()) {
                consumeOne(stack, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide && crop.applyWater() && !player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide && crop.harvest(player)) {
            return ItemInteractionResult.sidedSuccess(false);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CropTile crop)) {
            return InteractionResult.PASS;
        }
        if (crop.isEmpty() && crop.isCrossTile()) {
            if (!level.isClientSide) {
                crop.removeCross(player);
                level.playSound(
                        null,
                        pos,
                        SoundType.WOOD.getPlaceSound(),
                        SoundSource.BLOCKS,
                        0.5F,
                        0.8F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (crop.harvest(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * Plants a crop seed or a matching base seed on an empty stick.
     * A cross refuses the seed and tells the player why.
     */
    static ItemInteractionResult offerSeed(
            Level level, CropTile crop, ItemStack stack, Player player) {
        CropCard card = seedCard(stack);
        if (card == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!crop.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (crop.isCrossTile()) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.gt6crops.no_plant_on_cross"),
                        true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            CropRules.Stats stats = CropSeedItem.data(stack)
                    .map(CropSeedItem.SeedData::stats)
                    .orElseGet(() -> CropRules.initialStats(
                            card, new Random(level.random.nextLong())));
            crop.plant(card, stats);
            consumeOne(stack, player);
            level.playSound(
                    null,
                    crop.getBlockPos(),
                    SoundEvents.CROP_PLANTED,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static CropCard seedCard(ItemStack stack) {
        CropCard fromSeed = CropSeedItem.card(stack).orElse(null);
        if (fromSeed != null && !fromSeed.isWeed()) {
            return fromSeed;
        }
        return CropCards.byBaseSeed(stack).orElse(null);
    }

    private static void consumeOne(ItemStack stack, Player player) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
