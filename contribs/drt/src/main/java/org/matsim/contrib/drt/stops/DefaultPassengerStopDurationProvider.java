package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public class DefaultPassengerStopDurationProvider implements PassengerStopDurationProvider {
	private final double stopDuration;

	public DefaultPassengerStopDurationProvider(double stopDuration) {
		this.stopDuration = stopDuration;
	}

	@Override
	public double calcPickupDuration(DvrpVehicle vehicle, DrtRequest request) {
		// pickups happen after this time from the stop beginning
		return stopDuration;
	}

	@Override
	public double calcDropoffDuration(DvrpVehicle vehicle, DrtRequest request) {
		// dropoffs happen at the beginning of the stop, but extend the stop duration by
		// this amount
		return stopDuration;
	}
}
