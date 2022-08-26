package org.matsim.contrib.shared_mobility.service.events;

import org.matsim.core.events.handler.EventHandler;

public interface SharingReservingEventHandler extends EventHandler {
	void handleEvent(SharingReservingEvent event);
}