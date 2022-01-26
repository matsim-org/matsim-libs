package org.matsim.contrib.sharing.service.events;

import org.matsim.core.events.handler.EventHandler;

public interface SharingFailedPickupEventHandler extends EventHandler {
	void handleEvent(SharingFailedPickupEvent event);
}
