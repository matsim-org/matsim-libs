package org.matsim.contrib.drt.passenger;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public final class PromisedPickupTimeWindowDrtOfferAcceptor implements DrtOfferAcceptor {
	private final double promisedPickupTimeWindow;

	public PromisedPickupTimeWindowDrtOfferAcceptor(double promisedPickTimeWindow) {
		this.promisedPickupTimeWindow = promisedPickTimeWindow;
	}

	@Override
	public Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request, double departureTime, double arrivalTime) {
		double updatedPickupTimeWindow = Math.min(departureTime
				+ promisedPickupTimeWindow, request.getLatestStartTime());
		return Optional.of(AcceptedDrtRequest.newBuilder().latestStartTime(updatedPickupTimeWindow).build());
	}
}
