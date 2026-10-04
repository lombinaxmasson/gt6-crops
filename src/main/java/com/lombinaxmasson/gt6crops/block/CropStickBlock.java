package com.lombinaxmasson.gt6crops.block;

import org.jetbrains.annotations.Nullable;

import java.util.Random;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.Gt6CropsTags;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.item.SeedBagItem;
import com.lombinaxmasson.gt6crops.item.WeedExItem;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A single crop-stick host, with a crossbreed variant for empty centers. */
public final class CropStickBlock extends Block implements EntityBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    private final boolean crossTile;

    public CropStickBlock(boolean crossTile) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .instabreak()
                .sound(SoundType.BAMBOO)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY));
        this.crossTile = crossTile;
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    public boolean crossTile() {
        return crossTile;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AGE, 0);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.DIRT)
                || level.getBlockState(pos.below()).is(Blocks.FARMLAND);
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

        if (stack.getItem() instanceof WeedExItem) {
            if (!level.isClientSide && crop.applyWeedEx()) {
                consumeOne(stack, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (crop.isEmpty()) {
            SeedBagItem.data(stack).ifPresentOrElse(
                    data -> plantFromBag(level, crop, data, stack, player),
                    () -> plantFromBaseSeed(level, crop, stack, player));
            if (SeedBagItem.data(stack).isPresent()
                    || CropCards.byBaseSeed(stack).isPresent()) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
        if (level.getBlockEntity(pos) instanceof CropTile crop && crop.harvest(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static void plantFromBag(
            Level level,
            CropTile crop,
            SeedBagItem.SeedData data,
            ItemStack stack,
            Player player) {
        CropCard card = CropCards.find(data.cardId()).orElse(null);
        if (card == null) {
            return;
        }
        if (!level.isClientSide) {
            crop.plant(card, data.stats());
            consumeOne(stack, player);
        }
    }

    private static void plantFromBaseSeed(
            Level level, CropTile crop, ItemStack stack, Player player) {
        CropCards.byBaseSeed(stack).ifPresent(card -> {
            if (!level.isClientSide) {
                crop.plant(card, CropRules.initialStats(
                        card, new Random(level.random.nextLong())));
                consumeOne(stack, player);
            }
        });
    }

    private static void consumeOne(ItemStack stack, Player player) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
