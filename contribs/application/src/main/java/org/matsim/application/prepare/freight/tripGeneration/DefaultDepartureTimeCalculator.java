package org.matsim.application.prepare.freight.tripGeneration;

import java.util.Random;

class DefaultDepartureTimeCalculator implements FreightAgentGenerator.DepartureTimeCalculator {
    private final Random rnd = new Random(1111);

    @Override
    public double getDepartureTime() {
        return rnd.nextInt(24) * 3600.0;
    }
}
