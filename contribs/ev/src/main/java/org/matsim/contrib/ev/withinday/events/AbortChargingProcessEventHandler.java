package org.matsim.contrib.ev.withinday.events;

import org.matsim.core.events.handler.EventHandler;

public interface AbortChargingProcessEventHandler extends EventHandler {
	void handleEvent(AbortChargingProcessEvent event);
}
