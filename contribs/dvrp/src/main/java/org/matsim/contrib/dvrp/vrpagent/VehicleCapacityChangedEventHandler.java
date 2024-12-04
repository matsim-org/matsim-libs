package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author Tarek Chouaki (tkchouaki)
 */
public interface VehicleCapacityChangedEventHandler extends EventHandler {

	void handleEvent(VehicleCapacityChangedEvent event);
}
