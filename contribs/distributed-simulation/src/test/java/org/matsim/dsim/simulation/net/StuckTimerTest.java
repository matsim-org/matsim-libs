package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StuckTimerTest {

    @Test
    public void start() {

        var timer = new StuckTimer(42);
        timer.start(1);
        timer.start(2);

        // threshold is 42. We want the timer to be stuck after 1 + 42 = 43
        assertFalse(timer.isStuck(42));
        assertTrue(timer.isStuck(43));
    }

    @Test
    public void reset() {

        var timer = new StuckTimer(42);
        timer.start(17);
        assertTrue(timer.isStuck(17 + 42));

        timer.reset();
        assertFalse(timer.isStuck(17 + 42));
    }
}