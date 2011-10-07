/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author Ihab
 *
 */
public class LinksEventHandler implements LinkLeaveEventHandler, LinkEnterEventHandler {
	private double vehicleHours;
	private Network network;
	Map<Id,List<Id>> vehicleId2LinkIDs = new HashMap<Id, List<Id>>();
	
	/**
	 * @param network
	 */
	public LinksEventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		vehicleId2LinkIDs.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.vehicleId2LinkIDs.containsKey(event.getPersonId())){
			this.vehicleId2LinkIDs.get(event.getPersonId()).add(event.getLinkId());
		}
		else {
			List<Id> vehicleId2LinkIDs = new ArrayList<Id>();
			vehicleId2LinkIDs.add(event.getLinkId());
			this.vehicleId2LinkIDs.put(event.getPersonId(), vehicleId2LinkIDs);
		}
	}

	/**
	 * @return the vehicleKm
	 */
	public double getVehicleKm() {
		double vehicleKm = 0.0;
		for(Id vehicleId : this.vehicleId2LinkIDs.keySet()){
			for(Id linkId : this.vehicleId2LinkIDs.get(vehicleId)){
				vehicleKm = vehicleKm + ( (network.getLinks().get(linkId).getLength())/1000 );
			}
		}
		return vehicleKm;
	}

	/**
	 * @param vehicleHours the vehicleHours to set
	 */
	public void setVehicleHours(double vehicleHours) {
		this.vehicleHours = vehicleHours;
	}

	/**
	 * @return the vehicleHours
	 */
	public double getVehicleHours() {
		return vehicleHours;
	}

}
