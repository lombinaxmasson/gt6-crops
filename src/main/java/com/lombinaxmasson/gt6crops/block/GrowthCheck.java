package com.lombinaxmasson.gt6crops.block;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.lombinaxmasson.gt6crops.card.CropConditions;
import com.lombinaxmasson.gt6crops.card.RegistryRef;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Evaluates a card's planting conditions at one crop stick. */
public final class GrowthCheck {
    public enum Problem { SOIL, SUB_SOIL, TOO_DARK, TOO_BRIGHT }

    private GrowthCheck() {}

    public static boolean soilAccepts(CropConditions conditions, Level level, BlockPos pos) {
        return anyMatch(conditions.soil(), level.getBlockState(pos.below()));
    }

    /** An empty result means the crop can grow and be bred here. */
    public static Set<Problem> problems(CropConditions conditions, Level level, BlockPos pos) {
        Set<Problem> problems = EnumSet.noneOf(Problem.class);
        if (!soilAccepts(conditions, level, pos)) {
            problems.add(Problem.SOIL);
        }
        if (!conditions.subSoil().isEmpty()
                && !anyMatch(conditions.subSoil(), level.getBlockState(pos.below(2)))) {
            problems.add(Problem.SUB_SOIL);
        }
        int light = level.getRawBrightness(pos, 0);
        if (conditions.tooDark(light)) {
            problems.add(Problem.TOO_DARK);
        }
        if (conditions.tooBright(light)) {
            problems.add(Problem.TOO_BRIGHT);
        }
        return problems;
    }

    public static int likedBiomes(CropConditions conditions, Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return (int) conditions.likedBiomes().stream().filter(liked -> liked.matches(biome)).count();
    }

    public static List<Component> describe(
            CropConditions conditions, Set<Problem> problems, Level level, BlockPos pos) {
        int light = level.getRawBrightness(pos, 0);
        List<Component> lines = new ArrayList<>();
        for (Problem problem : problems) {
            MutableComponent line = switch (problem) {
                case SOIL -> Component.translatable(
                        "message.gt6crops.inspect.needs_soil", join(conditions.soil()));
                case SUB_SOIL -> Component.translatable(
                        "message.gt6crops.inspect.needs_sub_soil", join(conditions.subSoil()));
                case TOO_DARK -> Component.translatable(
                        "message.gt6crops.inspect.too_dark", light, conditions.minLight());
                case TOO_BRIGHT -> Component.translatable(
                        "message.gt6crops.inspect.too_bright", light, conditions.maxLight());
            };
            lines.add(line.withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    public static Component join(List<? extends RegistryRef<?>> refs) {
        MutableComponent joined = Component.empty();
        for (int index = 0; index < refs.size(); index++) {
            if (index > 0) {
                joined.append(Component.translatable("message.gt6crops.or"));
            }
            joined.append(refs.get(index).describe());
        }
        return joined;
    }

    private static boolean anyMatch(List<RegistryRef<Block>> refs, BlockState state) {
        for (RegistryRef<Block> ref : refs) {
            if (ref.matches(state.getBlockHolder())) {
                return true;
            }
        }
        return false;
    }
}
