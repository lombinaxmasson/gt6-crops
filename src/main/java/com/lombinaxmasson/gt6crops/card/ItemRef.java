package com.lombinaxmasson.gt6crops.card;

import java.util.Locale;
import java.util.Optional;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
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
            MaterialPrefix resolvedPrefix = prefix(prefix);
            GTMaterial resolvedMaterial = GTMaterialRegistry.get(material);
            if (resolvedPrefix == null || !resolvedMaterial.isValid()) {
                return Optional.empty();
            }
            ItemStack stack = GTItems.getStack(resolvedPrefix, resolvedMaterial, count);
            return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
        }
        return Optional.empty();
    }

    private Optional<ItemStack> resolveItem(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        ResourceLocation location = ResourceLocation.parse(id);
        return BuiltInRegistries.ITEM.getOptional(location)
                .map(itemValue -> new ItemStack(itemValue, count));
    }

    private static MaterialPrefix prefix(String raw) {
        String value = raw.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        return switch (value) {
            case "plantgtberry" -> MaterialPrefix.plantGtBerry;
            case "plantgtblossom" -> MaterialPrefix.plantGtBlossom;
            case "plantgtfiber" -> MaterialPrefix.plantGtFiber;
            case "plantgttwig" -> MaterialPrefix.plantGtTwig;
            case "plantgtwart" -> MaterialPrefix.plantGtWart;
            case "dust" -> MaterialPrefix.dust;
            case "tinydust", "dusttiny" -> MaterialPrefix.dustTiny;
            case "nugget" -> MaterialPrefix.nugget;
            case "chunk", "chunkgt" -> MaterialPrefix.chunkGt;
            case "ingot" -> MaterialPrefix.ingot;
            case "stick" -> MaterialPrefix.stick;
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
