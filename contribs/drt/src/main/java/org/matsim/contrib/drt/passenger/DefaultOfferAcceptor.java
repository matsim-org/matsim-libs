package org.matsim.contrib.drt.passenger;

import java.util.Optional;

public class DefaultOfferAcceptor implements DrtOfferAcceptor{
	private final double maxAllowedPickupDelay;

	/**
	 * Generate Default offer acceptor with max allowed pickup delay.
	 * @param maxAllowedPickupDelay: maximum allowed delay since the initially assigned pickup time.
	 */
	public DefaultOfferAcceptor(double maxAllowedPickupDelay) {
		this.maxAllowedPickupDelay = maxAllowedPickupDelay;
	}

	/**
	 * Generate Default offer acceptor. 
	 */
	public DefaultOfferAcceptor() {
		this.maxAllowedPickupDelay = Double.POSITIVE_INFINITY;
	}

	@Override
	public Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request, double departureTime, double arrivalTime) {
		double updatedLatestStartTime = Math.min(departureTime
			+ maxAllowedPickupDelay, request.getLatestStartTime());
		return Optional.of(AcceptedDrtRequest
			.newBuilder()
			.request(request)
			.earliestStartTime(request.getEarliestStartTime())
			.maxRideDuration(request.getMaxRideDuration())
			.latestArrivalTime(Math.min(updatedLatestStartTime + request.getMaxRideDuration(), request.getLatestArrivalTime()))
			.latestStartTime(updatedLatestStartTime)
			.build());
	}
}
