package com.lombinaxmasson.gt6crops.client.emi;

import com.lombinaxmasson.gt6crops.Gt6Crops;

import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.render.EmiTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Tab icons. A 16×16 file at {@code assets/gt6crops/textures/gui/emi/<name>.png}
 * replaces the crop stick. The art brief is in the README under Recipe viewer.
 * Checked on each EMI reload, so adding the file does not need a new build of this class.
 */
final class CropEmiIcons {
    private CropEmiIcons() {}

    static EmiRenderable get(String name) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                Gt6Crops.MODID, "textures/gui/emi/" + name + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            return new EmiTexture(texture, 0, 0, 16, 16, 16, 16, 16, 16);
        }
        return CropEmiStacks.stick();
    }
}
