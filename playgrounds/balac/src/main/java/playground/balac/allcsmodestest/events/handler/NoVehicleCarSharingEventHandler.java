package playground.balac.allcsmodestest.events.handler;


import org.matsim.core.events.handler.EventHandler;

import playground.balac.allcsmodestest.events.NoVehicleCarSharingEvent;

public interface NoVehicleCarSharingEventHandler extends EventHandler {
	
	public void handleEvent (NoVehicleCarSharingEvent event);
	
}