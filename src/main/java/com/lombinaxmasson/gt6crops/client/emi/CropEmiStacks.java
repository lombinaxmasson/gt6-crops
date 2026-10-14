package com.lombinaxmasson.gt6crops.client.emi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.ItemRef;
import com.lombinaxmasson.gt6crops.card.RegistryRef;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** Crop seeds and soil blocks as EMI stacks. Seed comparison ignores stats, so any stack of a crop matches. */
final class CropEmiStacks {
    private static final CropRules.Stats DISPLAY_STATS = new CropRules.Stats(1, 1, 1);

    private CropEmiStacks() {}

    /**
     * A named seed for the index and the pages. The stat line is hidden: these
     * numbers are not what planting or a cross produces.
     */
    static EmiStack seed(CropCard card) {
        ItemStack stack = CropSeedItem.create(card, DISPLAY_STATS, true);
        stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return EmiStack.of(stack);
    }

    static String cardId(EmiStack stack) {
        ItemStack item = stack.getItemStack();
        if (item.isEmpty()) {
            return "";
        }
        return CropSeedItem.data(item).map(CropSeedItem.SeedData::cardId).orElse("");
    }

    static EmiStack stick() {
        return EmiStack.of(Gt6Crops.CROP_STICK_ITEM.get());
    }

    /** Every block the condition accepts, as items. Empty when none of them have an item. */
    static EmiIngredient blocks(List<RegistryRef<Block>> refs) {
        List<EmiStack> stacks = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        for (RegistryRef<Block> ref : refs) {
            if (ref.tag()) {
                for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(ref.tagKey())) {
                    addBlock(stacks, seen, holder.value());
                }
            } else {
                BuiltInRegistries.BLOCK.getOptional(ref.id())
                        .ifPresent(block -> addBlock(stacks, seen, block));
            }
        }
        if (stacks.isEmpty()) {
            return EmiStack.EMPTY;
        }
        return stacks.size() == 1 ? stacks.get(0) : EmiIngredient.of(stacks);
    }

    /**
     * The harvest, then each special drop. The page paints CropsNH's yellow chance
     * itself; a chance set on the stack would draw EMI's own overlay on top of it.
     */
    static List<EmiStack> harvest(CropCard card) {
        List<EmiStack> result = new ArrayList<>();
        card.harvestDrop().map(EmiStack::of).ifPresent(result::add);
        for (ItemRef special : card.specialDrops()) {
            special.resolve().ifPresent(stack -> result.add(EmiStack.of(stack)));
        }
        return result;
    }

    static String safe(String raw) {
        String folded = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return folded.isEmpty() ? "_" : folded;
    }

    private static void addBlock(List<EmiStack> stacks, Set<Item> seen, Block block) {
        Item item = block.asItem();
        if (item != Items.AIR && seen.add(item)) {
            stacks.add(EmiStack.of(item));
        }
    }
}
