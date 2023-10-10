package org.matsim.contrib.drt.extension.prebooking.dvrp;

import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * Helper class to decouple event processing from PrebookingStopActivity.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PassengerEnteringHandler {
	private final EventsManager eventsManager;
	private final String mode;

	public PassengerEnteringHandler(EventsManager eventsManager, String mode) {
		this.mode = mode;
		this.eventsManager = eventsManager;
	}

	public void sendEnteringEvent(double now, AcceptedDrtRequest request) {
		eventsManager.processEvent(new PassengerEnteringEvent(now, mode, request.getId(), request.getPassengerId()));
	}
}
