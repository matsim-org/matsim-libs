package org.matsim.contrib.freight.events.eventhandler;

import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.core.events.handler.EventHandler;

/**
 * Interface to listen to shipmentDeliveredEvents.
 * 
 * @author sschroeder
 *
 */
public interface ShipmentDeliveredEventHandler extends EventHandler {

	public void handleEvent(ShipmentDeliveredEvent event);

}
