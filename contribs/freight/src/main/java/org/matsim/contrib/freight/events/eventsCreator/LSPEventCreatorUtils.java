package org.matsim.contrib.freight.events.eventsCreator;

import java.util.ArrayList;
import java.util.Collection;

public final class LSPEventCreatorUtils {

	public static Collection<LSPEventCreator> getStandardEventCreators(){
		ArrayList<LSPEventCreator> creators = new ArrayList<>();
		creators.add(new LSPFreightLinkEnterEventCreator());
		creators.add(new LSPFreightLinkLeaveEventCreator());
		creators.add(new LSPFreightVehicleLeavesTrafficEventCreator());
		creators.add(new LSPServiceEndEventCreator());
		creators.add(new LSPServiceStartEventCreator());
		creators.add(new LSPShipmentDeliveredEventCreator());
		creators.add(new LSPShipmentPickedUpEventCreator());
		creators.add(new LSPTourEndEventCreator());
		creators.add(new LSPTourStartEventCreator());
		return creators;
	}
	
}
