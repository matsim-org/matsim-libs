package org.matsim.contrib.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface ShipmentDeliveredEventHandler extends EventHandler {

	public void handleEvent(ShipmentDeliveredEvent event);

}
