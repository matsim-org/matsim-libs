package org.matsim.contrib.carsharing.events.handlers;

import org.matsim.contrib.carsharing.events.NoParkingSpaceEvent;
import org.matsim.core.events.handler.EventHandler;

public interface NoParkingSpotEventHandler extends EventHandler {
	
	public void handleEvent (NoParkingSpaceEvent event);
	
}