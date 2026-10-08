package com.lombinaxmasson.gt6crops.item;

import java.util.List;
import java.util.Optional;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/**
 * One seed item for every crop. The species and the three genetic values live
 * in the stack's custom-data component, the same way CropsNH stores them on
 * its generic seed.
 */
public final class CropSeedItem extends Item {
    private static final String CARD = "Card";
    private static final String GROWTH = "Growth";
    private static final String GAIN = "Gain";
    private static final String RESISTANCE = "Resistance";

    public CropSeedItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(CropCard card, CropRules.Stats stats) {
        ItemStack stack = new ItemStack(Gt6Crops.CROP_SEED.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(CARD, card.id());
        tag.putInt(GROWTH, stats.growth());
        tag.putInt(GAIN, stats.gain());
        tag.putInt(RESISTANCE, stats.resistance());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static Optional<SeedData> data(ItemStack stack) {
        if (!(stack.getItem() instanceof CropSeedItem)) {
            return Optional.empty();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Optional.empty();
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(CARD)) {
            return Optional.empty();
        }
        return Optional.of(new SeedData(
                tag.getString(CARD),
                new CropRules.Stats(
                        tag.getInt(GROWTH),
                        tag.getInt(GAIN),
                        tag.getInt(RESISTANCE))));
    }

    public static Optional<CropCard> card(ItemStack stack) {
        return data(stack).flatMap(value -> CropCards.find(value.cardId()));
    }

    @Override
    public Component getName(ItemStack stack) {
        return card(stack)
                .map(value -> Component.translatable(
                        "item.gt6crops.crop_seed.named", value.name()))
                .orElseGet(() -> Component.translatable("item.gt6crops.crop_seed"));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        data(stack).ifPresent(value -> {
            CropRules.Stats stats = value.stats();
            tooltip.add(Component.translatable(
                    "item.gt6crops.crop_seed.stats",
                    stats.growth(), stats.gain(), stats.resistance()));
        });
    }

    public record SeedData(String cardId, CropRules.Stats stats) {}
}
