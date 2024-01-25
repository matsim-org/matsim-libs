package org.matsim.contrib.drt.extension.insertion;

import java.util.Optional;

import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtOfferAcceptor;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * This class is based on MOIA's PromisedPickupTimeWindowDrtOfferAcceptor.
 */
public class PromisedTimeWindowOfferAcceptor implements DrtOfferAcceptor {
	private final double promisedPickupTimeWindow;
	private final double promisedDropoffTimeWindow;

	public PromisedTimeWindowOfferAcceptor(double promisedPickTimeWindow, double promisedDropoffTimeWindow) {
		this.promisedPickupTimeWindow = promisedPickTimeWindow;
		this.promisedDropoffTimeWindow = promisedDropoffTimeWindow;
	}

	@Override
	public Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request, double departureTime, double arrivalTime) {
		double updatedPickupTimeWindow = Math.min(departureTime + promisedPickupTimeWindow,
				request.getLatestStartTime());

		double updatedDropoffTimeWindow = Math.min(arrivalTime + promisedDropoffTimeWindow,
				request.getLatestArrivalTime());

		return Optional
				.of(AcceptedDrtRequest.newBuilder().request(request).earliestStartTime(request.getEarliestStartTime())
						.latestArrivalTime(updatedDropoffTimeWindow).latestStartTime(updatedPickupTimeWindow).build());
	}
}
