package org.matsim.contrib.drt.passenger;

import java.util.Optional;

public class MaxDetourOfferAcceptor implements DrtOfferAcceptor{
	private final double maxAllowedPickupDelay;

	public MaxDetourOfferAcceptor(double maxAllowedPickupDelay) {
		this.maxAllowedPickupDelay = maxAllowedPickupDelay;
	}

	@Override
	public Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request, double departureTime, double arrivalTime) {
		double updatedLatestStartTime = Math.min(departureTime
			+ maxAllowedPickupDelay, request.getLatestStartTime());
		return Optional.of(AcceptedDrtRequest
			.newBuilder()
			.request(request)
			.earliestStartTime(request.getEarliestStartTime())
			.latestArrivalTime(Math.min(updatedLatestStartTime + request.getMaxRideDuration(), request.getLatestArrivalTime()))
			.latestStartTime(updatedLatestStartTime).build());
	}
}
