package com.lombinaxmasson.gt6crops.machine;

import java.util.Locale;

import com.gregtech.gregtech.api.energy.GTVoltageTiers;

/**
 * GT6 electric tiers. As with GT6CE's own basic machines, a machine takes
 * packets from half to twice its voltage and explodes above that.
 */
public enum MachineTier {
    /** GT6CE builds LV electric machines from galvanized steel. */
    LV(1, 0xFAF0F0);

    private final int index;
    private final int casingColor;

    MachineTier(int index, int casingColor) {
        this.index = index;
        this.casingColor = casingColor;
    }

    /** GT's tier number: 1 for LV. CropsNH scales its machines by it. */
    public int index() {
        return index;
    }

    public long voltage() {
        return GTVoltageTiers.VOLTAGES[index];
    }

    public long inputMin() {
        return voltage() / 2;
    }

    public long inputMax() {
        return voltage() * 2;
    }

    public String displayName() {
        return GTVoltageTiers.NAMES[index];
    }

    /** The material color GT6CE tints this tier's casing with. */
    public int casingColor() {
        return casingColor;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
