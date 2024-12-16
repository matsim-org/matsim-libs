package org.matsim.contrib.ev.withinday.events;

import org.matsim.core.events.handler.EventHandler;

public interface AbortChargingAttemptEventHandler extends EventHandler {
	void handleEvent(AbortChargingAttemptEvent event);
}
