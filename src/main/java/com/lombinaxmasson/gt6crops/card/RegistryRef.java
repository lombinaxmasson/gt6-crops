package com.lombinaxmasson.gt6crops.card;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.Tags;

/** One entry of a card condition list: a registry id, or a tag written as {@code #namespace:path}. */
public record RegistryRef<T>(
        ResourceKey<? extends Registry<T>> registry,
        ResourceLocation id,
        boolean tag) {

    public static <T> RegistryRef<T> parse(ResourceKey<? extends Registry<T>> registry, String text) {
        boolean tag = text.startsWith("#");
        return new RegistryRef<>(registry, ResourceLocation.parse(tag ? text.substring(1) : text), tag);
    }

    public TagKey<T> tagKey() {
        return TagKey.create(registry, id);
    }

    public boolean matches(Holder<T> holder) {
        return tag ? holder.is(tagKey()) : holder.is(id);
    }

    public Component describe() {
        String path = registry.location().getPath();
        String key = tag
                ? Tags.getTagTranslationKey(tagKey())
                : Util.makeDescriptionId(path.substring(path.lastIndexOf('/') + 1), id);
        return Component.translatableWithFallback(key, toString());
    }

    @Override
    public String toString() {
        return (tag ? "#" : "") + id;
    }
}
