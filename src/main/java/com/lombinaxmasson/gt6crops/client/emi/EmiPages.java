package com.lombinaxmasson.gt6crops.client.emi;

import java.util.Locale;

import com.lombinaxmasson.gt6crops.Gt6Crops;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Slot diagrams cropped from CropsNH's NEI sheets. Each PNG is the diagram
 * itself, not a 256×256 sheet, and an item position is the top-left of the
 * 16×16 gray hole. EMI's slot is 18×18 and draws the icon one pixel in, so the
 * slot widget sits one pixel up and left, with its own frame left off.
 */
final class EmiPages {
    /** CropsNH's {@code 1644054}, with the alpha 1.21 needs or the text vanishes. */
    static final int TEXT = 0xFF191616;
    static final int LINE = 10;
    /** CropsNH's yellow chance overlay, {@code 0xFDD835}. */
    private static final int CHANCE = 0xFFFDD835;
    private static final float CHANCE_SCALE = 0.45F;

    static final ResourceLocation BREEDING_2 = sheet("breeding2");
    static final ResourceLocation BREEDING_4 = sheet("breeding4");
    static final ResourceLocation CROP_LIST = sheet("crop_list");
    static final ResourceLocation MUTATION_POOL = sheet("mutation_pool");
    static final ResourceLocation ALTERNATE_SEEDS = sheet("alternate_seeds");

    /** Lines under the diagrams are scaled to this, wider than the narrowest picture. */
    static final int BREEDING_WIDTH = 166;
    static final int CROP_WIDTH = 166;
    static final int POOL_WIDTH = 166;
    static final int ALTERNATE_WIDTH = 59;
    static final int ALTERNATE_HEIGHT = 18;

    static final int BREEDING_2_W = 80;
    static final int BREEDING_4_W = 122;
    static final int BREED_ART_H = 75;
    /** Two parents: left, right. The child is the center column. */
    static final int[] BREED2_PARENT_X = {1, 63};
    static final int BREED2_CENTER_X = 32;
    /** Three or four parents, in CropsNH's order: inner left, inner right, outer left, outer right. */
    static final int[] BREED4_PARENT_X = {22, 84, 1, 105};
    static final int BREED4_CENTER_X = 53;
    static final int BREED_Y_SEED = 16;
    static final int BREED_Y_SOIL = 37;
    static final int BREED_Y_BASE = 58;
    static final int BREED_TEXT_Y = BREED_ART_H + 2;

    static final int CROP_ART_W = 140;
    static final int CROP_ART_H = 75;
    static final int CROP_X_SEED = 16;
    static final int CROP_Y_SEED = 16;
    static final int CROP_Y_SOIL = 37;
    static final int CROP_Y_BASE = 58;
    static final int CROP_X_PRODUCT = 87;
    static final int CROP_Y_PRODUCT = 12;
    static final int CROP_COLUMNS = 3;
    static final int CROP_TEXT_Y = CROP_ART_H + 2;

    static final int POOL_X = 1;
    static final int POOL_Y = 1;
    static final int POOL_COLUMNS = 9;
    private static final int POOL_TEX_W = 162;
    private static final int POOL_TEX_H = 38;

    static final int ALT_INPUT_X = 1;
    static final int ALT_OUTPUT_X = 42;
    static final int ALT_Y = 1;

    private EmiPages() {}

    /** Draws one of these sheets at its real pixel size. They are not 256×256. */
    static void image(WidgetHolder widgets, ResourceLocation texture, int width, int height) {
        widgets.addTexture(texture, 0, 0, width, height, 0, 0, width, height, width, height);
    }

    /**
     * The pool strip is 162×38: a 9px cap, a blank row, an 18px middle, a blank row,
     * and a 9px cap. One row of members is the two caps. Each further row inserts the middle.
     */
    static void poolFrame(WidgetHolder widgets, int rows) {
        int y = 0;
        poolTile(widgets, y, 0, 9);
        y += 9;
        for (int row = 1; row < rows; row++) {
            poolTile(widgets, y, 10, 18);
            y += 18;
        }
        poolTile(widgets, y, 29, 9);
    }

    private static void poolTile(WidgetHolder widgets, int y, int v, int height) {
        widgets.addTexture(
                MUTATION_POOL, 0, y, POOL_TEX_W, height, 0, v, POOL_TEX_W, height, POOL_TEX_W, POOL_TEX_H);
    }

    static int poolRows(int members) {
        return Math.max(1, members / POOL_COLUMNS + (members % POOL_COLUMNS == 0 ? 0 : 1));
    }

    static int poolNameY(int rows) {
        return rows * 18 + 2;
    }

    static SlotWidget item(WidgetHolder widgets, EmiIngredient stack, int x, int y) {
        return widgets.addSlot(stack, x - 1, y - 1).drawBack(false);
    }

    /** CropsNH's {@code drawFixesWidthLine}: one line, squeezed on X when it is wider than {@code maxWidth}. */
    static void line(WidgetHolder widgets, Component text, int x, int y, int maxWidth) {
        widgets.addDrawable(x, y, maxWidth, LINE, (graphics, mouseX, mouseY, delta) -> {
            Font font = Minecraft.getInstance().font;
            int width = font.width(text);
            float scale = width > maxWidth ? (float) maxWidth / width : 1F;
            graphics.pose().pushPose();
            graphics.pose().scale(scale, 1F, 1F);
            graphics.drawString(font, text, 0, 0, TEXT, false);
            graphics.pose().popPose();
        });
    }

    /**
     * CropsNH's chance label, top-left of the icon, scaled to 0.45.
     * {@code hundredths} is the unit its formatter uses: 100% is 10000, so one in four is 2500.
     */
    static void chance(WidgetHolder widgets, int itemX, int itemY, int hundredths) {
        String label = String.format(Locale.ROOT, "%d.%02d%%", hundredths / 100, hundredths % 100);
        widgets.addDrawable(0, 0, 0, 0, (graphics, mouseX, mouseY, delta) -> {
            Font font = Minecraft.getInstance().font;
            int alignX = -1;
            int alignY = -1;
            int textWidth = font.width(label);
            int x = (int) ((itemX + 8 + 8 * alignX) / CHANCE_SCALE) - (textWidth / 2 * (alignX + 1));
            int y = (int) ((itemY + 8 + 8 * alignY) / CHANCE_SCALE)
                    - (font.lineHeight / 2 * (alignY + 1))
                    - (alignY - 1) / 2;
            graphics.pose().pushPose();
            graphics.pose().scale(CHANCE_SCALE, CHANCE_SCALE, 1F);
            graphics.drawString(font, label, x, y, CHANCE, false);
            graphics.pose().popPose();
        });
    }

    private static ResourceLocation sheet(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                Gt6Crops.MODID, "textures/gui/nei/" + name + ".png");
    }
}
