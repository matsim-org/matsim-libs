package org.matsim.contrib.ev.withinday.events;

import org.matsim.core.events.handler.EventHandler;

public interface StartChargingProcessEventHandler extends EventHandler {
	void handleEvent(StartChargingProcessEvent event);
}
