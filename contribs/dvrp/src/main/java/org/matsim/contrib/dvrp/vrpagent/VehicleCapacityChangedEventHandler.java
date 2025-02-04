package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public interface VehicleCapacityChangedEventHandler extends EventHandler {
	void handleEvent(VehicleCapacityChangedEvent event);
}
