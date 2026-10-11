package com.lombinaxmasson.gt6crops.machine;

import com.gregtech.gregtech.api.machine.BasicMachineEnergy;

/**
 * GT6CE's basic-machine power rules. The buffer holds at most one tick of the
 * tier's highest input and is drained every tick, so a machine only runs while
 * a cable keeps feeding it.
 */
public final class MachineEnergy {
    private static final float MIN_EXPLOSION = 3.0F;
    private static final float MAX_EXPLOSION = 8.0F;

    private final MachineTier tier;
    private long stored;

    public MachineEnergy(MachineTier tier) {
        this.tier = tier;
    }

    public long stored() {
        return stored;
    }

    public boolean isOvervoltage(long size) {
        return Math.abs(size) > tier.inputMax();
    }

    /** Packets of {@code size} EU the buffer still has room for. */
    public long demanded(long size) {
        return BasicMachineEnergy.demanded(stored, tier.inputMax(), size);
    }

    /** Takes up to {@code amount} packets of {@code size} EU and returns how many it took. */
    public long accept(long size, long amount, boolean doAccept) {
        long accepted = BasicMachineEnergy.accepted(stored, tier.inputMax(), size, amount);
        if (doAccept && accepted > 0) {
            stored = BasicMachineEnergy.add(stored, tier.inputMax(), size, accepted);
        }
        return accepted;
    }

    /** Returns this tick's energy and drains it, used or not. */
    public long drain() {
        long available = stored;
        stored = BasicMachineEnergy.drain(stored, tier.inputMax());
        return available;
    }

    public void clear() {
        stored = 0;
    }

    public void load(long value) {
        stored = Math.max(0, Math.min(tier.inputMax(), value));
    }

    /** GT6CE: the blast grows with how far the packet exceeds the highest input. */
    public float explosionPower(long size) {
        double over = (double) Math.abs(size) / Math.max(1, tier.inputMax());
        float power = MIN_EXPLOSION + (float) (Math.log(over) / Math.log(2.0));
        return Math.max(MIN_EXPLOSION, Math.min(MAX_EXPLOSION, power));
    }
}
