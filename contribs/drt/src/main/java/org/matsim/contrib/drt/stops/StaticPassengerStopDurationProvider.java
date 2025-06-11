package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public class StaticPassengerStopDurationProvider implements PassengerStopDurationProvider {
	private final double pickupDuration;
	private final double dropoffDuration;

	public StaticPassengerStopDurationProvider(double pickupDuration, double dropoffDuration) {
		this.pickupDuration = pickupDuration;
		this.dropoffDuration = dropoffDuration;
	}

	@Override
	public double calcPickupDuration(DvrpVehicle vehicle, DrtRequest request) {
		return pickupDuration;
	}

	@Override
	public double calcDropoffDuration(DvrpVehicle vehicle, DrtRequest request) {
		return dropoffDuration;
	}

	public static StaticPassengerStopDurationProvider of(double pickupDuration, double dropoffDuration) {
		return new StaticPassengerStopDurationProvider(pickupDuration, dropoffDuration);
	}
}
