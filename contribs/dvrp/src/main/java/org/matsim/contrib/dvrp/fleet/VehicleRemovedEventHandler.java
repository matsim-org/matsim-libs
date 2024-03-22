package org.matsim.contrib.dvrp.fleet;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author Sebastian Hörl (sebhoerl), IRT Systemx
 */
public interface VehicleRemovedEventHandler extends EventHandler {
	void handleEvent(VehicleRemovedEvent event);
}
