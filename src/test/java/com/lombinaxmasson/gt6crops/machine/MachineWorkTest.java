package com.lombinaxmasson.gt6crops.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MachineWorkTest {
    @Test
    void lvCopiesAtTheRecipePower() {
        MachineWork.Cost cost = seedCopy(MachineTier.LV);
        assertEquals(30, cost.minimumPower());
        assertEquals(4800, cost.totalWork());
    }

    @Test
    void higherTiersQuadruplePowerUntilTheMinimum() {
        assertEquals(120, seedCopy(MachineTier.MV).minimumPower());
        assertEquals(9600, seedCopy(MachineTier.MV).totalWork());
        assertEquals(480, seedCopy(MachineTier.HV).minimumPower());
        assertEquals(19200, seedCopy(MachineTier.HV).totalWork());
    }

    @Test
    void aRecipeAboveTheInputMaximumIsRefused() {
        assertNull(MachineWork.calculate(65, 1, 1, true, 10_000, 16, 64, false, false));
    }

    @Test
    void advanceStopsAtTheJobTotal() {
        assertEquals(40, MachineWork.advance(10, 40, 100));
    }

    private static MachineWork.Cost seedCopy(MachineTier tier) {
        return MachineWork.calculate(30, 160, 1, true, 10_000, tier.inputMin(), tier.inputMax(), false, false);
    }
}
