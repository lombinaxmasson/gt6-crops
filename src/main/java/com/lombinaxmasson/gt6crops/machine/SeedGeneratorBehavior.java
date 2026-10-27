package com.lombinaxmasson.gt6crops.machine;

import java.util.Optional;

import gregtech.api.addon.platform.MachineBehavior;
import gregtech.api.addon.platform.MachineBehaviorContext;
import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * CropsNH's Seed Generator on GregTech's behavior shell. It copies a scanned crop seed,
 * keeping the original, for enriched fertilizer by the seed's total stats and, for some
 * crops, a catalyst item.
 *
 * <p>It works as GT6CE's basic machines do: the seed's fertilizer and catalyst are
 * spent when a copy starts, and the copy needs power on every tick. A tick without
 * enough starts it over, though nothing spent is lost. The pending copy, work and
 * power live in {@code BehaviorState}, so a reload keeps them and a break drops the copy.</p>
 */
public final class SeedGeneratorBehavior implements MachineBehavior {
    public static final int INPUT_SLOTS = 2;
    public static final int OUTPUT_SLOT = INPUT_SLOTS;
    public static final int SLOTS = OUTPUT_SLOT + 1;
    public static final int FERTILIZER_TANK = 0;

    /** CropsNH's LV recipe: 30 EU/t for 8 seconds. */
    private static final long RECIPE_EUT = 30;
    private static final long RECIPE_TICKS = 160;

    @Override
    public void tick(MachineBehaviorContext context) {
        MachineTier tier = MachineTier.byIndex(context.tier());
        ItemStack pending = context.state().item("pending");
        long work = context.state().getLong("work");
        long totalWork = context.state().getLong("total");
        long minimumPower = context.state().getLong("minimum");
        if (context.energy() < tier.inputMin()) {
            if (!pending.isEmpty() && work < totalWork) {
                context.state().setLong("work", 0);
                context.progress(0);
            }
            return;
        }
        if (pending.isEmpty()) {
            if (!start(context, tier)) {
                context.progress(0);
                return;
            }
            pending = context.state().item("pending");
            work = context.state().getLong("work");
            totalWork = context.state().getLong("total");
            minimumPower = context.state().getLong("minimum");
        }
        if (work < totalWork) {
            if (context.energy() < minimumPower) {
                context.state().setLong("work", 0);
                context.progress(0);
                return;
            }
            work = MachineWork.advance(work, totalWork, Math.min(context.energy(), tier.inputMax()));
            context.state().setLong("work", work);
            context.progress(totalWork <= 0 ? 0 : work / (float) totalWork);
            context.working(true);
            if (work < totalWork) {
                return;
            }
        }
        if (!outputFits(context, pending)) {
            context.progress(totalWork <= 0 ? 0 : work / (float) totalWork);
            return;
        }
        context.insertOutput(pending);
        context.state().setItem("pending", ItemStack.EMPTY);
        context.state().setLong("work", 0);
        context.state().setLong("total", 0);
        context.state().setLong("minimum", 0);
        context.progress(0);
        context.working(true);
    }

    @Override
    public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) {
        return slot < OUTPUT_SLOT;
    }

    @Override
    public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) {
        return tank == FERTILIZER_TANK && stack != null && stack.is(Gt6Crops.ENRICHED_FERTILIZER.source());
    }

    /** Spends what the copy needs and readies it, if any input seed can be copied now. */
    private static boolean start(MachineBehaviorContext context, MachineTier tier) {
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack seed = context.item(slot);
            Optional<CropSeedItem.SeedData> data = CropSeedItem.data(seed).filter(CropSeedItem.SeedData::scanned);
            Optional<CropCard> card = data.flatMap(value -> CropCards.find(value.cardId()))
                    .filter(value -> !value.isWeed() && value.canDuplicate());
            if (card.isEmpty()) {
                continue;
            }
            int cost = SeedGeneratorRules.fertilizerCost(data.get().stats());
            ItemStack copy = seed.copyWithCount(1);
            MachineWork.Cost recipe = MachineWork.calculate(
                    RECIPE_EUT, RECIPE_TICKS, 1, true, 10_000, tier.inputMin(), tier.inputMax(), false, false);
            if (recipe == null || context.fluid(FERTILIZER_TANK).getAmount() < cost
                    || !outputFits(context, copy) || !spendCatalyst(context, card.get(), true)) {
                continue;
            }
            spendCatalyst(context, card.get(), false);
            context.drain(FERTILIZER_TANK, cost, false);
            context.state().setItem("pending", copy);
            context.state().setLong("work", 0);
            context.state().setLong("total", recipe.totalWork());
            context.state().setLong("minimum", recipe.minimumPower());
            return true;
        }
        return false;
    }

    /** CropsNH takes the first of the card's catalysts the input slots hold enough of. */
    private static boolean spendCatalyst(MachineBehaviorContext context, CropCard card, boolean simulate) {
        if (card.duplicationCatalysts().isEmpty()) {
            return true;
        }
        for (ItemStack catalyst : card.resolvedCatalysts()) {
            if (held(context, catalyst) < catalyst.getCount()) {
                continue;
            }
            if (!simulate) {
                int left = catalyst.getCount();
                for (int slot = 0; slot < INPUT_SLOTS && left > 0; slot++) {
                    ItemStack stack = context.item(slot);
                    if (ItemStack.isSameItem(stack, catalyst)) {
                        int taken = Math.min(left, stack.getCount());
                        shrink(context, slot, taken);
                        left -= taken;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private static int held(MachineBehaviorContext context, ItemStack catalyst) {
        int count = 0;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = context.item(slot);
            if (ItemStack.isSameItem(stack, catalyst)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean outputFits(MachineBehaviorContext context, ItemStack copy) {
        ItemStack slot = context.item(OUTPUT_SLOT);
        if (slot.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(slot, copy)
                && slot.getCount() + copy.getCount() <= slot.getMaxStackSize();
    }

    private static void shrink(MachineBehaviorContext context, int slot, int count) {
        ItemStack stack = context.item(slot);
        int left = stack.getCount() - count;
        context.setItem(slot, left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left));
    }
}
