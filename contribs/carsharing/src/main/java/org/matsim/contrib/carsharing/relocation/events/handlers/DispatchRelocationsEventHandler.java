package org.matsim.contrib.carsharing.relocation.events.handlers;

import org.matsim.contrib.carsharing.relocation.events.DispatchRelocationsEvent;
import org.matsim.core.events.handler.EventHandler;


public interface DispatchRelocationsEventHandler extends EventHandler {
	public void handleEvent(DispatchRelocationsEvent event);
}
