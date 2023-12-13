package org.matsim.contrib.drt.passenger;

import java.util.Optional;

public class MaxDetourOfferAcceptor implements DrtOfferAcceptor{
	private final double promisedPickupTimeWindow;

	public MaxDetourOfferAcceptor(double promisedPickupTimeWindow) {
		this.promisedPickupTimeWindow = promisedPickupTimeWindow;
	}

	@Override
	public Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request, double departureTime, double arrivalTime) {
		double updatedPickupTimeWindow = Math.min(departureTime
			+ promisedPickupTimeWindow, request.getLatestStartTime());
		return Optional.of(AcceptedDrtRequest
			.newBuilder()
			.request(request)
			.earliestStartTime(request.getEarliestStartTime())
			.latestArrivalTime(Math.min(updatedPickupTimeWindow + request.getMaxRideDuration(), request.getLatestArrivalTime()))
			.latestStartTime(updatedPickupTimeWindow).build());
	}
}
