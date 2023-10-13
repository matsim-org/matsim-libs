package org.matsim.contrib.drt.extension.prebooking.dvrp;

import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * Helper class to decouple event processing from PrebookingStopActivity.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PassengerEnteringVehicleHandler {
	private final EventsManager eventsManager;
	private final String mode;

	public PassengerEnteringVehicleHandler(EventsManager eventsManager, String mode) {
		this.mode = mode;
		this.eventsManager = eventsManager;
	}

	public void sendEnteringEvent(double now, AcceptedDrtRequest request) {
		eventsManager.processEvent(new PassengerEnteringVehicleEvent(now, mode, request.getId(), request.getPassengerId()));
	}
}
