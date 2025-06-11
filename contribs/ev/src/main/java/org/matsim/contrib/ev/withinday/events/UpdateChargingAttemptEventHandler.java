package org.matsim.contrib.ev.withinday.events;

import org.matsim.core.events.handler.EventHandler;

public interface UpdateChargingAttemptEventHandler extends EventHandler {
	void handleEvent(UpdateChargingAttemptEvent event);
}
