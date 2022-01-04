package org.matsim.contrib.freight.events.eventhandler;

import org.matsim.contrib.freight.events.LSPFreightVehicleLeavesTrafficEvent;
import org.matsim.core.events.handler.EventHandler;

public interface LSPVehicleLeavesTrafficEventHandler extends EventHandler {
	
	public void handleEvent( LSPFreightVehicleLeavesTrafficEvent event );
}
