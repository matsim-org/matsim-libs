package org.matsim.api.core.v01.events.handler;

import org.matsim.api.core.v01.events.PersonContinuesInVehicleEvent;
import org.matsim.core.events.handler.EventHandler;

public interface PersonContinuesInVehicleEventHandler extends EventHandler {
	void handleEvent(PersonContinuesInVehicleEvent event);
}
