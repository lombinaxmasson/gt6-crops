package com.lombinaxmasson.gt6crops.card;

import java.util.List;

import com.lombinaxmasson.gt6crops.Gt6Crops;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** Logs cards whose soil or sub-soil matches no block, since those crops can never grow. */
@EventBusSubscriber(modid = Gt6Crops.MODID)
public final class CropConditionWarnings {
    private CropConditionWarnings() {}

    @SubscribeEvent
    public static void warnAboutUnmatchedSoils(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD
                || !CropCards.initialized()) {
            return;
        }
        for (CropCard card : CropCards.cards()) {
            if (card.isWeed()) {
                continue;
            }
            warnIfNothingMatches(card, "soil", card.conditions().soil());
            warnIfNothingMatches(card, "sub_soil", card.conditions().subSoil());
        }
    }

    private static void warnIfNothingMatches(
            CropCard card, String field, List<RegistryRef<Block>> refs) {
        if (refs.isEmpty() || refs.stream().anyMatch(CropConditionWarnings::matchesSomeBlock)) {
            return;
        }
        Gt6Crops.LOGGER.warn("gt6crops: no block matches the {} {} of crop card {}; it can never grow",
                field, refs, card.id());
    }

    private static boolean matchesSomeBlock(RegistryRef<Block> ref) {
        return ref.tag()
                ? BuiltInRegistries.BLOCK.getTag(ref.tagKey()).map(set -> set.size() > 0).orElse(false)
                : BuiltInRegistries.BLOCK.containsKey(ref.id());
    }
}
