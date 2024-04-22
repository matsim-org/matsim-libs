package org.matsim.contrib.dvrp.fleet;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author Sebastian Hörl (sebhoerl), IRT Systemx
 */
public interface VehicleAddedEventHandler extends EventHandler {
	void handleEvent(VehicleAddedEvent event);
}
