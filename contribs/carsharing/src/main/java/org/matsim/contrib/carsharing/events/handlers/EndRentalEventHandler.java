package org.matsim.contrib.carsharing.events.handlers;

import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.core.events.handler.EventHandler;

public interface EndRentalEventHandler extends EventHandler{
	
	public void handleEvent (EndRentalEvent event);
	
}
