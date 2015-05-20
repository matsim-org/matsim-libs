package org.matsim.contrib.carsharing.events.handlers;

import org.matsim.contrib.carsharing.events.CarsharingLegFinishedEvent;
import org.matsim.core.events.handler.EventHandler;

public interface CarsharingLegFinishedEvenHandler extends EventHandler{

	public void handleEvent (CarsharingLegFinishedEvent event);


}
