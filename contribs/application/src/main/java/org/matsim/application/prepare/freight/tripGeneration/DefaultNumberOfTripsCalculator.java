package org.matsim.application.prepare.freight.tripGeneration;

import java.util.Random;

public class DefaultNumberOfTripsCalculator implements FreightAgentGenerator.NumOfTripsCalculator {
    private final Random rnd = new Random(1234);
	private final double maxLoad_heavyTruck = 27.0;
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
	@Override
	public int calculateNumberOfTripsV2(double tonsPerYear, String goodsType) {
		double trips;
		if (LongDistanceFreightUtils.findTransportType(Integer.parseInt(goodsType)) == LongDistanceFreightUtils.TransportType.FTL)
			trips = sample * tonsPerYear / (workingDays * maxLoad_heavyTruck);
		else
			trips = sample * tonsPerYear / (workingDays * averageLoad);
		trips = Math.floor(trips + rnd.nextDouble());
		return (int) trips;
	}
}
