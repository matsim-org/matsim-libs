package org.matsim.contrib.freight.events.eventhandler;

import org.matsim.contrib.freight.events.LSPFreightVehicleLeavesTrafficEvent;

public interface LSPVehicleLeavesTrafficEventHandler{
	
	public void handleEvent( LSPFreightVehicleLeavesTrafficEvent event );
}
