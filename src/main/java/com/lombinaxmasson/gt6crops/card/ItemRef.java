package com.lombinaxmasson.gt6crops.card;

import java.util.Locale;
import java.util.Optional;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.platform.AddonItems;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A data-driven reference to either a vanilla item, a GT material form, or a
 * GT item whose name is known at data-authoring time.
 */
public record ItemRef(
        String kind,
        String item,
        String prefix,
        String material,
        int count) {

    public static final ItemRef NONE = new ItemRef("none", null, null, null, 1);

    public static ItemRef fromJson(JsonObject json) {
        if (json == null) {
            return NONE;
        }
        return new ItemRef(
                string(json, "kind", "none"),
                nullableString(json, "item"),
                nullableString(json, "prefix"),
                nullableString(json, "material"),
                Math.max(1, json.has("count") ? json.get("count").getAsInt() : 1));
    }

    public boolean isNone() {
        return "none".equals(kind)
                || "external".equals(kind)
                || "external_arsmagica".equals(kind)
                || "unparsed".equals(kind);
    }

    public boolean matches(ItemStack stack) {
        return resolve().map(expected -> expected.getItem() == stack.getItem()).orElse(false);
    }

    public Optional<ItemStack> resolve() {
        if (isNone()) {
            return Optional.empty();
        }
        if ("vanilla".equals(kind) || "item".equals(kind) || "gt_item".equals(kind)) {
            return resolveItem(item);
        }
        if ("foods_addon".equals(kind)) {
            return resolveItem(item);
        }
        if ("gt_form".equals(kind) || "plant_form".equals(kind) || "material_form".equals(kind)) {
            if (prefix == null || material == null) {
                return Optional.empty();
            }
            String resolvedPrefix = prefixName(prefix);
            GregTechMaterials.Material resolvedMaterial = gtMaterial();
            if (resolvedPrefix == null || !resolvedMaterial.valid()) {
                return Optional.empty();
            }
            ItemStack stack = AddonItems.stack(resolvedPrefix, resolvedMaterial.name(), count);
            return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
        }
        return Optional.empty();
    }

    /** The named GT material, or the invalid sentinel. */
    public GregTechMaterials.Material gtMaterial() {
        return material == null ? GregTechMaterials.invalid() : material(material);
    }

    private Optional<ItemStack> resolveItem(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        ResourceLocation location = ResourceLocation.parse(id);
        return BuiltInRegistries.ITEM.getOptional(location)
                .map(itemValue -> new ItemStack(itemValue, count));
    }

    /**
     * GT material names keep their original capitals. Ledger names are folded
     * so {@code live_root} resolves to {@code LiveRoot}.
     */
    private static GregTechMaterials.Material material(String raw) {
        GregTechMaterials.Material direct = GregTechMaterials.find(raw);
        if (direct.valid()) {
            return direct;
        }
        String folded = fold(raw);
        for (GregTechMaterials.Material candidate : GregTechMaterials.all()) {
            if (candidate.valid() && fold(candidate.name()).equals(folded)) {
                return candidate;
            }
        }
        return direct;
    }

    private static String fold(String value) {
        return value.replace("_", "").replace("-", "").replace(" ", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String prefixName(String raw) {
        String value = raw.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        return switch (value) {
            case "plantgtberry" -> "plantGtBerry";
            case "plantgtblossom" -> "plantGtBlossom";
            case "plantgtfiber" -> "plantGtFiber";
            case "plantgttwig" -> "plantGtTwig";
            case "plantgtwart" -> "plantGtWart";
            case "dust" -> "dust";
            case "tinydust", "dusttiny" -> "dustTiny";
            case "gem" -> "gem";
            case "nugget" -> "nugget";
            case "chunk", "chunkgt" -> "chunkGt";
            case "ingot" -> "ingot";
            case "stick" -> "stick";
            default -> null;
        };
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString()
                : fallback;
    }

    private static String nullableString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString()
                : null;
    }
}
