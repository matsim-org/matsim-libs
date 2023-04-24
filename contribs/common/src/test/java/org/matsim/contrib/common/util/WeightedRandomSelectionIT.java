package org.matsim.contrib.common.util;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.matsim.core.gbl.MatsimRandom;

public class WeightedRandomSelectionIT {

    private WeightedRandomSelection<String> weightedRandomSelection;

    @Before
    public void init() {
        Random random = MatsimRandom.getLocalInstance();
        weightedRandomSelection = new WeightedRandomSelection<>(random);
    }

    @Test
    public void testZeroCumulativeWeight() {
        weightedRandomSelection.add("0", 0.);
        weightedRandomSelection.add("1", 0.);
        weightedRandomSelection.add("2", 0.);
        weightedRandomSelection.select(); // throws exception now
    }

}
