/* *********************************************************************** *
 * project: org.matsim.*
 * TransitEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.ikaddoura.busCorridor.version4;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

/**
 * @author Ihab
 *
 */
public class TransitEventHandler implements TransitDriverStartsEventHandler {
	private List<Id> vehicleIDs = new ArrayList<Id>();
	
	@Override
	public void reset(int iteration) {
		this.vehicleIDs.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		Id vehicleId = event.getVehicleId();
		if (vehicleIDs.contains(vehicleId)){
			// vehicleID bereits in Liste
		}
		else{
			this.vehicleIDs.add(vehicleId);
		}
	}

	public List<Id> getVehicleIDs() {
		return vehicleIDs;
	}
	
}
