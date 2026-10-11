package com.lombinaxmasson.gt6crops.command;

import com.lombinaxmasson.gt6crops.block.CropTile;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.rules.CropRules;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Op-only helpers for testing crop growth, weeds and disease without waiting
 * for the 256-tick growth cycle.
 */
public final class Gt6CropsCommands {
    private static final SimpleCommandExceptionType NO_CROP_STICK = new SimpleCommandExceptionType(
            Component.literal("Look at a crop stick"));
    private static final SimpleCommandExceptionType EMPTY_STICK = new SimpleCommandExceptionType(
            Component.literal("That crop stick is empty"));
    private static final SimpleCommandExceptionType UNKNOWN_CARD = new SimpleCommandExceptionType(
            Component.literal("Unknown crop card"));

    private static final SuggestionProvider<CommandSourceStack> CARD_IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    CropCards.cards().stream().map(CropCard::id),
                    builder);

    private Gt6CropsCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gt6crops")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("tick")
                        .executes(context -> tick(context, 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 10_000))
                                .executes(context -> tick(
                                        context, IntegerArgumentType.getInteger(context, "count")))))
                .then(Commands.literal("mature")
                        .executes(Gt6CropsCommands::mature))
                .then(Commands.literal("setsize")
                        .then(Commands.argument("size", IntegerArgumentType.integer(1, 7))
                                .executes(context -> setSize(
                                        context, IntegerArgumentType.getInteger(context, "size")))))
                .then(Commands.literal("setstats")
                        .then(Commands.argument("growth", IntegerArgumentType.integer(0, CropRules.MAX_STAT))
                                .then(Commands.argument("gain", IntegerArgumentType.integer(0, CropRules.MAX_STAT))
                                        .then(Commands.argument(
                                                        "resistance",
                                                        IntegerArgumentType.integer(0, CropRules.MAX_STAT))
                                                .executes(Gt6CropsCommands::setStats)))))
                .then(Commands.literal("fill")
                        .executes(Gt6CropsCommands::fill))
                .then(Commands.literal("plant")
                        .then(Commands.argument("card", StringArgumentType.word())
                                .suggests(CARD_IDS)
                                .executes(Gt6CropsCommands::plant)
                                .then(Commands.argument(
                                                "growth", IntegerArgumentType.integer(0, CropRules.MAX_STAT))
                                        .then(Commands.argument(
                                                        "gain", IntegerArgumentType.integer(0, CropRules.MAX_STAT))
                                                .then(Commands.argument(
                                                                "resistance",
                                                                IntegerArgumentType.integer(0, CropRules.MAX_STAT))
                                                        .executes(Gt6CropsCommands::plantWithStats)))))));
    }

    private static int tick(CommandContext<CommandSourceStack> context, int count)
            throws CommandSyntaxException {
        CropTile crop = lookedAt(context.getSource());
        int gained = crop.forceGrowthCycles(count);
        context.getSource().sendSuccess(
                () -> Component.literal("Ran " + count + " growth cycle(s); size +" + gained
                        + " → " + describe(crop)),
                true);
        return count;
    }

    private static int mature(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CropTile crop = planted(context.getSource());
        CropCard card = crop.card().orElseThrow(EMPTY_STICK::create);
        crop.setSize(card.maxSize());
        context.getSource().sendSuccess(
                () -> Component.literal("Matured → " + describe(crop)),
                true);
        return 1;
    }

    private static int setSize(CommandContext<CommandSourceStack> context, int size)
            throws CommandSyntaxException {
        CropTile crop = planted(context.getSource());
        if (!crop.setSize(size)) {
            throw EMPTY_STICK.create();
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Size set → " + describe(crop)),
                true);
        return size;
    }

    private static int setStats(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CropTile crop = planted(context.getSource());
        CropRules.Stats stats = new CropRules.Stats(
                IntegerArgumentType.getInteger(context, "growth"),
                IntegerArgumentType.getInteger(context, "gain"),
                IntegerArgumentType.getInteger(context, "resistance"));
        if (!crop.setStats(stats)) {
            throw EMPTY_STICK.create();
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Stats set → " + describe(crop)),
                true);
        return 1;
    }

    private static int fill(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CropTile crop = lookedAt(context.getSource());
        crop.fillStorage();
        context.getSource().sendSuccess(
                () -> Component.literal("Filled storage on " + describe(crop)),
                true);
        return 1;
    }

    private static int plant(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        return plantCard(context, new CropRules.Stats(1, 1, 1));
    }

    private static int plantWithStats(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        return plantCard(context, new CropRules.Stats(
                IntegerArgumentType.getInteger(context, "growth"),
                IntegerArgumentType.getInteger(context, "gain"),
                IntegerArgumentType.getInteger(context, "resistance")));
    }

    private static int plantCard(
            CommandContext<CommandSourceStack> context, CropRules.Stats stats)
            throws CommandSyntaxException {
        CropTile crop = lookedAt(context.getSource());
        String id = StringArgumentType.getString(context, "card");
        CropCard card = CropCards.find(id).orElseThrow(UNKNOWN_CARD::create);
        crop.plant(card, card.isWeed() ? CropRules.WEED_STATS : stats, true);
        context.getSource().sendSuccess(
                () -> Component.literal("Planted → " + describe(crop)),
                true);
        return 1;
    }

    private static CropTile planted(CommandSourceStack source) throws CommandSyntaxException {
        CropTile crop = lookedAt(source);
        if (crop.isEmpty()) {
            throw EMPTY_STICK.create();
        }
        return crop;
    }

    private static CropTile lookedAt(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(player.blockInteractionRange()));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            throw NO_CROP_STICK.create();
        }
        BlockPos pos = hit.getBlockPos();
        if (!(player.level().getBlockEntity(pos) instanceof CropTile crop)) {
            throw NO_CROP_STICK.create();
        }
        return crop;
    }

    private static String describe(CropTile crop) {
        return crop.card()
                .map(card -> card.id() + " size " + crop.size() + "/" + card.maxSize()
                        + " stats " + crop.stats().growth() + "/" + crop.stats().gain()
                        + "/" + crop.stats().resistance()
                        + (crop.isSick() ? " sick" : "")
                        + (crop.isScanned() ? "" : " unscanned"))
                .orElse(crop.isCrossTile() ? "empty cross" : "empty stick");
    }
}
