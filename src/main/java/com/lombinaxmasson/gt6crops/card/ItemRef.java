package com.lombinaxmasson.gt6crops.card;

import java.util.Locale;
import java.util.Optional;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialView;
import gregtech.api.addon.platform.AddonItems;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A data-driven reference to a registered item or a GT material form. */
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

    public boolean isCropProduct() {
        return "crop_product".equals(kind)
                || ("gt_form".equals(kind) && prefix != null && prefix.startsWith("plantGt"));
    }

    public Optional<ItemStack> resolve() {
        if (isNone()) {
            return Optional.empty();
        }
        if ("vanilla".equals(kind) || "item".equals(kind) || "gt_item".equals(kind)
                || "foods_addon".equals(kind) || "crop_product".equals(kind)) {
            return resolveItem(item);
        }
        if ("gt_form".equals(kind) || "plant_form".equals(kind) || "material_form".equals(kind)) {
            if (prefix == null) {
                return Optional.empty();
            }
            String resolvedPrefix = prefixName(prefix);
            if (resolvedPrefix == null) {
                return Optional.empty();
            }
            return gtMaterial()
                    .map(key -> AddonItems.stack(resolvedPrefix, key.name(), count))
                    .filter(stack -> !stack.isEmpty());
        }
        return Optional.empty();
    }

    /** Resolve GT aliases first, then fold ledger names such as {@code live_root} to {@code LiveRoot}. */
    public Optional<MaterialKey> gtMaterial() {
        if (material == null) {
            return Optional.empty();
        }
        var catalog = GregTechMaterials.catalog();
        return catalog.resolve(material).or(() -> {
            String folded = fold(material);
            return catalog.materials().stream()
                    .map(MaterialView::key)
                    .filter(key -> fold(key.name()).equals(folded))
                    .findFirst();
        });
    }

    private Optional<ItemStack> resolveItem(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        ResourceLocation location = ResourceLocation.parse(id);
        return BuiltInRegistries.ITEM.getOptional(location)
                .map(itemValue -> new ItemStack(itemValue, count));
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
