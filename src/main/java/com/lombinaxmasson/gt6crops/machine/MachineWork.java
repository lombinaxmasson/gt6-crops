package com.lombinaxmasson.gt6crops.machine;

import java.math.BigInteger;

/**
 * GT6 basic-machine work, kept here because the host moved the calculator out of the addon API.
 * The seed generator uses the same inputs GregTech's basic machines do: ordinary overclocking,
 * efficiency in parts per 10,000, and no time-energy mode.
 */
public final class MachineWork {
    public record Cost(long minimumPower, long totalWork) {}

    private MachineWork() {}

    /** Completed work, capped at {@code total}. Extra supplied work is discarded. */
    public static long advance(long completed, long total, long supplied) {
        long limit = Math.max(0, total);
        long progress = Math.min(limit, Math.max(0, completed));
        return progress + Math.min(limit - progress, Math.max(0, supplied));
    }

    /**
     * Power and total work for one job. Returns null when the inputs have no representable cost.
     * A recipe above the machine's maximum is refused. When cheap overclocking is off, power
     * quadruples and work doubles until the power reaches the machine's minimum.
     */
    public static Cost calculate(long recipePower, long duration, int parallel, boolean scaleDuration,
                                 int efficiency, long minimumInput, long maximumInput,
                                 boolean cheapOverclocking, boolean timeEnergy) {
        if (recipePower < 0 || recipePower > maximumInput || duration < 1 || parallel < 1 || efficiency < 1) {
            return null;
        }

        long power = Math.max(1, recipePower);
        if (!scaleDuration && !timeEnergy && recipePower > 0) {
            if (power > maximumInput / parallel) {
                return null;
            }
            power *= parallel;
        }

        BigInteger work = BigInteger.valueOf(power).multiply(BigInteger.valueOf(duration))
                .multiply(BigInteger.valueOf(scaleDuration ? parallel : 1)).multiply(BigInteger.valueOf(10000))
                .add(BigInteger.valueOf(efficiency - 1L)).divide(BigInteger.valueOf(efficiency));

        if (!cheapOverclocking) {
            while (power < minimumInput && power <= maximumInput / 4) {
                power *= 4;
                work = work.multiply(BigInteger.TWO);
            }
        }

        if (work.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            return null;
        }
        return new Cost(power, Math.max(1, work.longValue()));
    }
}
