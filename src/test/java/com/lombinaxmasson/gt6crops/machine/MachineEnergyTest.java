package com.lombinaxmasson.gt6crops.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void onlyPacketsAboveTwiceTheVoltageAreOvervoltage() {
        MachineEnergy energy = new MachineEnergy(MachineTier.LV);

        assertFalse(energy.isOvervoltage(64));
        assertTrue(energy.isOvervoltage(65));
    }

    @Test
    void bufferHoldsOneTickOfTheHighestInput() {
        MachineEnergy energy = new MachineEnergy(MachineTier.LV);

        assertEquals(2, energy.accept(32, 5, true));
        assertEquals(64, energy.stored());
        assertEquals(0, energy.demanded(32));
    }

    @Test
    void simulatedPacketsAreNotStored() {
        MachineEnergy energy = new MachineEnergy(MachineTier.LV);

        assertEquals(1, energy.accept(32, 1, false));
        assertEquals(0, energy.stored());
    }

    @Test
    void drainEmptiesTheTicksEnergyWhetherUsedOrNot() {
        MachineEnergy energy = new MachineEnergy(MachineTier.LV);
        energy.accept(32, 1, true);

        assertEquals(32, energy.drain());
        assertEquals(0, energy.stored());
    }

    @Test
    void explosionGrowsWithTheOvervoltage() {
        MachineEnergy energy = new MachineEnergy(MachineTier.LV);

        assertEquals(3.0F, energy.explosionPower(65), 0.05F);
        assertEquals(6.0F, energy.explosionPower(512), 0.001F);
        assertEquals(8.0F, energy.explosionPower(8192), 0.001F);
    }
}
