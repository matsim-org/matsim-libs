package org.matsim.application.prepare.freight.tripGeneration;

import java.util.Random;

public class DefaultNumberOfTripsCalculator implements FreightAgentGenerator.NumOfTripsCalculator {
    private final Random rnd = new Random(1234);

    @Override
    public int calculateNumberOfTrips(double tonsPerYear, String goodsType) {
        double trips = tonsPerYear / (250 * 16); // TODO make this configurable
        trips = Math.floor(trips + rnd.nextDouble());
        return (int) trips;
    }
}
