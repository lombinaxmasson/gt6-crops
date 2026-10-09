package com.lombinaxmasson.gt6crops.card;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lombinaxmasson.gt6crops.DataFiles;
import com.lombinaxmasson.gt6crops.Gt6Crops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/**
 * Loads the crop-card ledger after GT6CE has linked its material items.
 */
public final class CropCards {
    private static final Gson GSON = new Gson();
    private static final Map<String, CropCard> CARDS = new LinkedHashMap<>();
    private static final List<String> SKIPPED = new ArrayList<>();
    private static final List<RegistryRef<Block>> SOILS = new ArrayList<>();
    private static boolean initialized;

    private CropCards() {}

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        JsonObject root = readLedger();
        for (JsonElement element : root.getAsJsonArray("crops")) {
            CropCard card = CropCard.fromJson(element.getAsJsonObject());
            if (!dependencyAvailable(element.getAsJsonObject())
                    || !usable(card)) {
                SKIPPED.add(card.id());
                Gt6Crops.LOGGER.warn("gt6crops: skipping unavailable crop card {}", card.id());
                continue;
            }
            if (CARDS.put(card.id(), card) != null) {
                throw new IllegalStateException("Duplicate crop card " + card.id());
            }
            card.conditions().soil().stream()
                    .filter(soil -> !SOILS.contains(soil))
                    .forEach(SOILS::add);
        }
        initialized = true;
        CropMutations.bind(List.copyOf(CARDS.keySet()));
        Gt6Crops.LOGGER.info("gt6crops: loaded {} crop cards; skipped {}",
                CARDS.size(), SKIPPED.size());
    }

    public static boolean initialized() {
        return initialized;
    }

    public static Collection<CropCard> cards() {
        initialize();
        return List.copyOf(CARDS.values());
    }

    public static Optional<CropCard> find(String id) {
        initialize();
        return Optional.ofNullable(CARDS.get(id));
    }

    public static Optional<CropCard> byBaseSeed(ItemStack stack) {
        initialize();
        for (CropCard card : CARDS.values()) {
            if (card.matchesBaseSeed(stack)) {
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    public static Optional<CropCard> weed() {
        return find("weed");
    }

    /** A crop stick can stand on any block that some loaded card accepts as soil. */
    public static boolean isSoil(BlockState state) {
        initialize();
        for (RegistryRef<Block> soil : SOILS) {
            if (soil.matches(state.getBlockHolder())) {
                return true;
            }
        }
        return false;
    }

    public static List<String> skipped() {
        initialize();
        return List.copyOf(SKIPPED);
    }

    private static boolean usable(CropCard card) {
        if (card.isWeed()) {
            return true;
        }
        if (card.harvestDrop().isEmpty()) {
            return false;
        }
        return card.crossbreedOnly() || card.hasPlantableBaseSeed();
    }

    private static boolean dependencyAvailable(JsonObject json) {
        if (!json.has("blocked_without") || json.get("blocked_without").isJsonNull()) {
            return true;
        }
        String dependency = json.get("blocked_without").getAsString();
        return switch (dependency) {
            case "arsmagica" -> ModList.get().isLoaded("arsmagica2")
                    || ModList.get().isLoaded("ars_nouveau");
            case "thaumcraft" -> ModList.get().isLoaded("thaumcraft");
            case "twilightforest" -> ModList.get().isLoaded("twilightforest");
            default -> ModList.get().isLoaded(dependency);
        };
    }

    private static JsonObject readLedger() {
        try (var stream = DataFiles.open("crop_cards.json")) {
            return GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    JsonObject.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load gt6crops crop cards", exception);
        }
    }
}
