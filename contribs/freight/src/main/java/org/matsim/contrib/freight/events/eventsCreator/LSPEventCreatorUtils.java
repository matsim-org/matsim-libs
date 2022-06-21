/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

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
