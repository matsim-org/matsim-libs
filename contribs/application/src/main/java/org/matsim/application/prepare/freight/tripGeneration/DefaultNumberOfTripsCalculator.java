package org.matsim.application.prepare.freight.tripGeneration;

import java.util.Random;

public class DefaultNumberOfTripsCalculator implements FreightAgentGenerator.NumOfTripsCalculator {
    private final Random rnd = new Random(1234);
    private final double averageLoad;
    private final int workingDays;
    private final double sample;

    public DefaultNumberOfTripsCalculator(double averageLoad, int workingDays, double sample) {
        this.averageLoad = averageLoad;
        this.workingDays = workingDays;
        this.sample = sample;
    }

    @Override
    public int calculateNumberOfTrips(double tonsPerYear, String goodsType) {
        double trips = sample * tonsPerYear / (workingDays * averageLoad);
        trips = Math.floor(trips + rnd.nextDouble());
        return (int) trips;
    }
}
