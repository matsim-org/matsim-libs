package org.matsim.contrib.freight.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * Interface to listen to shipmentPickedUpEvents.
 * 
 * @author sschroeder
 *
 */
public interface ShipmentPickedUpEventHandler extends EventHandler {
	public void handleEvent(ShipmentPickedUpEvent event);
}
