package org.matsim.contrib.emissions;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public interface LinkEmissionsCalculator{
	Map<WarmPollutant, Double> checkVehicleInfoAndCalculateWarmEmissions( Vehicle vehicle, Link link, double travelTime );
}
