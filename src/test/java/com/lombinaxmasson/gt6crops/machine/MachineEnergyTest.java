package com.lombinaxmasson.gt6crops.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachineEnergyTest {
    @Test
    void lvTakesHalfToTwiceItsVoltage() {
        assertEquals(32, MachineTier.LV.voltage());
        assertEquals(16, MachineTier.LV.inputMin());
        assertEquals(64, MachineTier.LV.inputMax());
        assertEquals("LV", MachineTier.LV.displayName());
    }

    @Test
    void tiersRunFromLvToIv() {
        assertEquals(5, MachineTier.values().length);
        assertEquals(128, MachineTier.MV.voltage());
        assertEquals(8192, MachineTier.IV.voltage());
        assertEquals(5, MachineTier.IV.index());
        assertEquals("iv", MachineTier.IV.id());
        assertEquals("SteelGalvanized", MachineTier.LV.casingMaterial());
        assertEquals("Titanium", MachineTier.IV.casingMaterial());
        assertEquals(MachineTier.HV, MachineTier.byIndex(3));
        assertEquals(MachineTier.EV, MachineTier.fromMachinePath("seed_generator_ev"));
    }
}
