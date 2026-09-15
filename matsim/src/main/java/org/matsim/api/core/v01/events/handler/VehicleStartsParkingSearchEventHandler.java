package org.matsim.api.core.v01.events.handler;

import org.matsim.api.core.v01.events.VehicleStartsParkingSearch;
import org.matsim.core.events.handler.EventHandler;

public interface VehicleStartsParkingSearchEventHandler extends EventHandler {
	public void handleEvent(VehicleStartsParkingSearch event);
}
