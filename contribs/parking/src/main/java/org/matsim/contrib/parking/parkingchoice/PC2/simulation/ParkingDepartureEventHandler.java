package org.matsim.contrib.parking.parkingchoice.PC2.simulation;

import org.matsim.core.events.handler.EventHandler;

public interface ParkingDepartureEventHandler extends EventHandler {
	public void handleEvent (ParkingDepartureEvent event);
}


