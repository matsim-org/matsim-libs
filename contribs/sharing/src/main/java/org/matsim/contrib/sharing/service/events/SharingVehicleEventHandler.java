package org.matsim.contrib.sharing.service.events;

import org.matsim.core.events.handler.EventHandler;

public interface SharingVehicleEventHandler extends EventHandler {
	void handleEvent(SharingVehicleEvent event);
}
