package org.matsim.contrib.shared_mobility.service.events;

import org.matsim.core.events.handler.EventHandler;

public interface SharingVehicleEventHandler extends EventHandler {
	void handleEvent(SharingVehicleEvent event);
}
