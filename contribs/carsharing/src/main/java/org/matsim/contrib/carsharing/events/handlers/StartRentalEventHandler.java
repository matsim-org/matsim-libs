package org.matsim.contrib.carsharing.events.handlers;

import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.core.events.handler.EventHandler;

public interface StartRentalEventHandler extends EventHandler {
	
	public void handleEvent (StartRentalEvent event);

}
