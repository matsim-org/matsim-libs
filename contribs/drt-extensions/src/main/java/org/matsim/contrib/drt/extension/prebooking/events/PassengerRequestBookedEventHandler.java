package org.matsim.contrib.drt.extension.prebooking.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface PassengerRequestBookedEventHandler extends EventHandler {
	void handleEvent(final PassengerRequestBookedEvent event);
}
