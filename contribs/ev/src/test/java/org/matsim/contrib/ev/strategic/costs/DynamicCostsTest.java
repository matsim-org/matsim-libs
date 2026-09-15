package org.matsim.contrib.ev.strategic.costs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.matsim.core.utils.misc.Time;

public class DynamicCostsTest {
    @Test
    public void testEmpty() {
        DynamicEnergyCosts costs = DynamicEnergyCosts.parse("2.0");

        double result = costs.calculate(Time.parseTime("01:00:00"), Time.parseTime("01:00:00"), 60.0);
        assertEquals(120.0, result); // 60 * 2
    }

    @Test
    public void testFirstSplit() {
        DynamicEnergyCosts costs = DynamicEnergyCosts.parse("1.0;02:00:00=2.0");

        double result = costs.calculate(Time.parseTime("01:30:00"), Time.parseTime("01:00:00"), 60.0);
        assertEquals(90.0, result); // 30 * 1 + 30 * 2
    }

    @Test
    public void testMultipleSplit() {
        DynamicEnergyCosts costs = DynamicEnergyCosts.parse("1.0;02:00:00=2.0;02:30:00=3.0");

        double result = costs.calculate(Time.parseTime("01:30:00"), Time.parseTime("02:00:00"), 120.0);
        assertEquals(270.0, result); // 30 * 1 + 30 * 2 + 60 * 3
    }

    @Test
    public void testBuilder() {
        DynamicEnergyCosts costs = new DynamicEnergyCosts.Builder() //
                .initial(1.0) //
                .breakpoint(2.0 * 3600.0, 2.0) //
                .breakpoint(2.5 * 3600.0, 3.0) //
                .build();

        double result = costs.calculate(Time.parseTime("01:30:00"), Time.parseTime("02:00:00"), 120.0);
        assertEquals(270.0, result); // 30 * 1 + 30 * 2 + 60 * 3
    }

    @Test
    public void testWrite() {
        String raw = "1.0;02:00:00=2.0;02:30:00=3.0";
        DynamicEnergyCosts costs = DynamicEnergyCosts.parse(raw);
        assertEquals(raw, DynamicEnergyCosts.write(costs));
    }
}
