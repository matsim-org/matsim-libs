package org.matsim.contrib.drt.passenger;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 * The function is now realized by the MaxDetourOfferAcceptor -Chengqi (luchengqi7)
 */
@Deprecated
public final class PromisedPickupTimeWindowDrtOfferAcceptor implements DrtOfferAcceptor {
	private final double promisedPickupTimeWindow;

	public PromisedPickupTimeWindowDrtOfferAcceptor(double promisedPickTimeWindow) {
		this.promisedPickupTimeWindow = promisedPickTimeWindow;
	}

	@Override
	public Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request, double departureTime, double arrivalTime) {
		double updatedPickupTimeWindow = Math.min(departureTime
				+ promisedPickupTimeWindow, request.getLatestStartTime());
		return Optional.of(AcceptedDrtRequest
				.newBuilder()
				.request(request)
				.earliestStartTime(request.getEarliestStartTime())
				.latestArrivalTime(request.getLatestArrivalTime())
				.latestStartTime(updatedPickupTimeWindow).build());
	}
}
