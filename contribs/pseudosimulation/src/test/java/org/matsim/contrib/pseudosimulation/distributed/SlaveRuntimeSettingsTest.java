package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlaveRuntimeSettingsTest {

    @Test
    void copiesEveryInitializationValueWithoutTranslation() {
        SlaveMasterSession.Initialization initialization = new SlaveMasterSession.Initialization(
                7, 3, 5, 0.25, 80, true, false, true, false, true, false);

        SlaveRuntimeSettings settings = SlaveRuntimeSettings.from(initialization);

        assertEquals(7, settings.number());
        assertEquals(3, settings.iterationsPerCycle());
        assertEquals(5, settings.numberOfPlans());
        assertEquals(0.25, settings.mutationRate());
        assertEquals(80, settings.lastIteration());
        assertTrue(settings.initialRouting());
        assertFalse(settings.quickReplanning());
        assertTrue(settings.fullTransitPerformance());
        assertFalse(settings.trackGenome());
        assertTrue(settings.intelligentRouters());
        assertFalse(settings.diversityGeneratingPlanSelection());
    }
}
