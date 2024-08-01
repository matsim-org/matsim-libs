package org.matsim.contrib.drt.prebooking.abandon;

import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public class MaximumDelayAbandonVoter implements AbandonVoter {
	private final double maximumDelay;

	public MaximumDelayAbandonVoter(double maximumDelay) {
		this.maximumDelay = maximumDelay;
	}

	@Override
	public boolean abandonRequest(double time, DvrpVehicle vehicle, AcceptedDrtRequest request) {
		double requestedDepartureTime = request.getEarliestStartTime();
		double delay = time - requestedDepartureTime;
		return delay > maximumDelay;
	}
}
