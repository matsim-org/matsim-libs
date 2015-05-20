package org.matsim.contrib.carsharing.events.handlers;


import org.matsim.contrib.carsharing.events.NoVehicleCarSharingEvent;
import org.matsim.core.events.handler.EventHandler;

public interface NoVehicleCarSharingEventHandler extends EventHandler {
	
	public void handleEvent (NoVehicleCarSharingEvent event);
	
}