package com.lombinaxmasson.gt6crops.client;

import java.util.ArrayList;
import java.util.List;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.machine.MachineMenu;
import com.lombinaxmasson.gt6crops.machine.MachineTier;
import com.lombinaxmasson.gt6crops.machine.MachineTile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Shared machine screen: the panel, the player's inventory, a frame under every
 * machine slot, and gauges that each machine places where its layout needs them.
 */
public abstract class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "textures/gui/machine.png");
    private static final int PANEL_HEIGHT = 166;
    /** Panel rows 3 to 82 are plain, so a taller machine area repeats them above the inventory. */
    private static final int PLAIN_V = 3;
    private static final int PLAIN_END = 83;
    private static final int SLOT_U = 176;
    private static final int SLOT_SIZE = 18;
    private static final int ENERGY_U = 194;
    private static final int ENERGY_FULL_U = 204;
    private static final int ENERGY_WIDTH = 10;
    private static final int GAUGE_HEIGHT = 54;
    private static final int TANK_U = 214;
    private static final int TANK_MARKS_U = 232;
    private static final int TANK_WIDTH = 18;
    private static final int ARROW_U = 176;
    private static final int ARROW_V = 54;
    private static final int ARROW_FULL_V = 71;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;
    private static final int TOGGLE_OFF_U = 176;
    private static final int TOGGLE_ON_U = 194;
    private static final int TOGGLE_V = 88;

    private record Hover(int x, int y, int width, int height, List<Component> lines) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record Toggle(Hover area, int id) {}

    private final List<Hover> hovers = new ArrayList<>();
    private final List<Toggle> toggles = new ArrayList<>();
    private final int extraHeight;

    /** {@code extraHeight} makes the machine area taller than vanilla's; the menu must move the inventory down to match. */
    protected MachineScreen(M menu, Inventory inventory, Component title, int extraHeight) {
        super(menu, inventory, title);
        this.extraHeight = extraHeight;
        this.imageHeight = PANEL_HEIGHT + extraHeight;
        this.inventoryLabelY = imageHeight - 94;
    }

    /**
     * Places the machine's gauges and switches with {@link #drawEnergy}, {@link #drawTank},
     * {@link #drawProgress} and {@link #drawToggle}.
     */
    protected abstract void drawGauges(GuiGraphics graphics);

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        hovers.clear();
        toggles.clear();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, PLAIN_END);
        int plainHeight = PLAIN_END - PLAIN_V;
        for (int drawn = 0; drawn < extraHeight; drawn += plainHeight) {
            graphics.blit(TEXTURE, leftPos, topPos + PLAIN_END + drawn,
                    0, PLAIN_V, imageWidth, Math.min(plainHeight, extraHeight - drawn));
        }
        graphics.blit(TEXTURE, leftPos, topPos + PLAIN_END + extraHeight,
                0, PLAIN_END, imageWidth, PANEL_HEIGHT - PLAIN_END);
        for (int i = 0; i < menu.machineSlotCount(); i++) {
            Slot slot = menu.slots.get(i);
            graphics.blit(TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, SLOT_U, 0, SLOT_SIZE, SLOT_SIZE);
        }
        drawGauges(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (Hover hover : hovers) {
            if (hover.contains(mouseX, mouseY)) {
                graphics.renderComponentTooltip(font, hover.lines(), mouseX, mouseY);
            }
        }
    }

    /** A bar of the EU that arrived last tick, against the most the tier takes. */
    protected void drawEnergy(GuiGraphics graphics, int x, int y) {
        MachineTier tier = menu.machine().tier();
        int left = leftPos + x;
        int top = topPos + y;
        graphics.blit(TEXTURE, left, top, ENERGY_U, 0, ENERGY_WIDTH, GAUGE_HEIGHT);
        int filled = scaled(menu.energy(), tier.inputMax(), GAUGE_HEIGHT);
        graphics.blit(TEXTURE, left, top + GAUGE_HEIGHT - filled,
                ENERGY_FULL_U, GAUGE_HEIGHT - filled, ENERGY_WIDTH, filled);
        hovers.add(new Hover(left, top, ENERGY_WIDTH, GAUGE_HEIGHT, List.of(
                Component.translatable("gui.gt6crops.machine.tier", tier.displayName(), tier.voltage()),
                Component.translatable("gui.gt6crops.machine.energy", menu.energy(), tier.inputMax()))));
    }

    /** One of the machine's tanks, drawn with its fluid's own texture and tint. */
    protected void drawTank(GuiGraphics graphics, int tankIndex, int x, int y) {
        MachineTile.Tank tank = menu.machine().tanks().get(tankIndex);
        int left = leftPos + x;
        int top = topPos + y;
        int amount = menu.tankAmount(tankIndex);
        graphics.blit(TEXTURE, left, top, TANK_U, 0, TANK_WIDTH, GAUGE_HEIGHT);
        int filled = scaled(amount, tank.getCapacity(), GAUGE_HEIGHT - 2);
        if (filled > 0) {
            IClientFluidTypeExtensions fluid = IClientFluidTypeExtensions.of(tank.fluid());
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(fluid.getStillTexture());
            int tint = fluid.getTintColor();
            graphics.setColor(
                    ((tint >> 16) & 0xFF) / 255.0F,
                    ((tint >> 8) & 0xFF) / 255.0F,
                    (tint & 0xFF) / 255.0F,
                    1.0F);
            int bottom = top + GAUGE_HEIGHT - 1;
            for (int drawn = 0; drawn < filled; drawn += 16) {
                int height = Math.min(16, filled - drawn);
                graphics.blit(left + 1, bottom - drawn - height, 0, TANK_WIDTH - 2, height, sprite);
            }
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        graphics.blit(TEXTURE, left, top, TANK_MARKS_U, 0, TANK_WIDTH, GAUGE_HEIGHT);
        hovers.add(new Hover(left, top, TANK_WIDTH, GAUGE_HEIGHT, List.of(
                new FluidStack(tank.fluid(), 1).getHoverName(),
                Component.translatable("gui.gt6crops.machine.tank", amount, tank.getCapacity()))));
    }

    /** An arrow that fills from left to right with the current job. */
    protected void drawProgress(GuiGraphics graphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        graphics.blit(TEXTURE, left, top, ARROW_U, ARROW_V, ARROW_WIDTH, ARROW_HEIGHT);
        int filled = Math.round(menu.progress() * ARROW_WIDTH);
        graphics.blit(TEXTURE, left, top, ARROW_U, ARROW_FULL_V, filled, ARROW_HEIGHT);
        hovers.add(new Hover(left, top, ARROW_WIDTH, ARROW_HEIGHT, List.of(
                Component.translatable("gui.gt6crops.machine.progress", Math.round(menu.progress() * 100)))));
    }

    /** A switch for one of the machine's jobs: raised when off, pressed in and green when on. */
    protected void drawToggle(GuiGraphics graphics, int toggle, int x, int y, ItemStack icon, Component name) {
        int left = leftPos + x;
        int top = topPos + y;
        boolean on = menu.isOn(toggle);
        graphics.blit(TEXTURE, left, top, on ? TOGGLE_ON_U : TOGGLE_OFF_U, TOGGLE_V, SLOT_SIZE, SLOT_SIZE);
        graphics.renderItem(icon, left + 1, top + 1);
        Hover area = new Hover(left, top, SLOT_SIZE, SLOT_SIZE, List.of(
                name,
                Component.translatable(on ? "gui.gt6crops.machine.on" : "gui.gt6crops.machine.off")));
        hovers.add(area);
        toggles.add(new Toggle(area, toggle));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Toggle toggle : toggles) {
                if (toggle.area().contains(mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, toggle.id());
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int scaled(long value, long max, int size) {
        if (value <= 0 || max <= 0) {
            return 0;
        }
        return (int) Math.max(1, Math.min(size, value * size / max));
    }
}
