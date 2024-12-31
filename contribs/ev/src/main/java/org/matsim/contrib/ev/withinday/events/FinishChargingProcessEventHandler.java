package org.matsim.contrib.ev.withinday.events;

import org.matsim.core.events.handler.EventHandler;

public interface FinishChargingProcessEventHandler extends EventHandler {
	void handleEvent(FinishChargingProcessEvent event);
}
