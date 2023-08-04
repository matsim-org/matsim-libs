package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public class StaticStopDurationProvider implements StopDurationProvider {
	private final double pickupDuration;
	private final double dropoffDuration;

	public StaticStopDurationProvider(double pickupDuration, double dropoffDuration) {
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

	public static StaticStopDurationProvider of(double pickupDuration, double dropoffDuration) {
		return new StaticStopDurationProvider(pickupDuration, dropoffDuration);
	}
}
