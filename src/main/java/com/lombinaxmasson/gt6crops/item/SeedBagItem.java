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
 * A seed bag carries the crop species and the three inherited genetic values
 * in the vanilla 1.21 custom-data component.
 */
public final class SeedBagItem extends Item {
    private static final String CARD = "Card";
    private static final String GROWTH = "Growth";
    private static final String GAIN = "Gain";
    private static final String RESISTANCE = "Resistance";

    public SeedBagItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(CropCard card, CropRules.Stats stats) {
        ItemStack stack = new ItemStack(Gt6Crops.SEED_BAG.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(CARD, card.id());
        tag.putInt(GROWTH, stats.growth());
        tag.putInt(GAIN, stats.gain());
        tag.putInt(RESISTANCE, stats.resistance());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static Optional<SeedData> data(ItemStack stack) {
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
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        data(stack).ifPresent(value -> {
            CropCards.find(value.cardId()).ifPresent(card ->
                    tooltip.add(Component.translatable(
                            "item.gt6crops.seed_bag.crop", card.name())));
            CropRules.Stats stats = value.stats();
            tooltip.add(Component.translatable(
                    "item.gt6crops.seed_bag.stats",
                    stats.growth(), stats.gain(), stats.resistance()));
        });
    }

    public record SeedData(String cardId, CropRules.Stats stats) {}
}
