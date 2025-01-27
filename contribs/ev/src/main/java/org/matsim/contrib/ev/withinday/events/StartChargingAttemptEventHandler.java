package org.matsim.contrib.ev.withinday.events;

import org.matsim.core.events.handler.EventHandler;

public interface StartChargingAttemptEventHandler extends EventHandler {
	void handleEvent(StartChargingAttemptEvent event);
}
