package playground.balac.allcsmodestest.events.handler;

import org.matsim.core.events.handler.EventHandler;

import playground.balac.allcsmodestest.events.NoParkingSpaceEvent;

public interface NoParkingSpotEventHandler extends EventHandler {
	
	public void handleEvent (NoParkingSpaceEvent event);
	
}