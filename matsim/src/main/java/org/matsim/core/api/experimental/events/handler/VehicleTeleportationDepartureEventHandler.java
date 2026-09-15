package org.matsim.core.api.experimental.events.handler;

import org.matsim.core.api.experimental.events.VehicleTeleportationDepartureEvent;
import org.matsim.core.events.handler.EventHandler;

public interface VehicleTeleportationDepartureEventHandler extends EventHandler {

	void handleEvent(VehicleTeleportationDepartureEvent event);

}
