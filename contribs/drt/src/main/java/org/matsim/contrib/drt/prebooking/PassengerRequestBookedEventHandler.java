package org.matsim.contrib.drt.prebooking;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface PassengerRequestBookedEventHandler extends EventHandler {
	void handleEvent(final PassengerRequestBookedEvent event);
}
