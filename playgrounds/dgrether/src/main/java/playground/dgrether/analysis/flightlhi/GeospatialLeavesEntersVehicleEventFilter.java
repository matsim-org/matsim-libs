/* *********************************************************************** *
 * project: org.matsim.*
 * GeospatialLeavesEntersVehicleEventFilter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.flightlhi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Network;

import playground.dgrether.events.filters.EventFilter;


/**
 * @author dgrether
 *
 */
public class GeospatialLeavesEntersVehicleEventFilter implements EventFilter {

	private Network network;
	private Map<Id, Boolean> networkContainedLastLinkEnterByVehicleId;
	private Set<Id> countedPersonIds;
	
	/**
	 * @param network
	 */
	public GeospatialLeavesEntersVehicleEventFilter(Network network) {
		this.network =network;
		this.networkContainedLastLinkEnterByVehicleId = new HashMap<Id, Boolean>();
		this.countedPersonIds = new HashSet<Id>();
	}

	
	
	@Override
	public boolean doProcessEvent(Event event) {
		Id linkId = null;
		Id vehId = null;
		if (event instanceof VehicleEntersTrafficEvent ) {
			VehicleEntersTrafficEvent e = (VehicleEntersTrafficEvent) event;
			linkId = e.getLinkId();
			vehId = e.getVehicleId();
		}
		else if (event instanceof LinkEnterEvent){
			LinkEnterEvent e = (LinkEnterEvent) event;
			linkId = e.getLinkId();
			vehId = e.getVehicleId();
		}
		else if (event instanceof PersonEntersVehicleEvent){
			PersonEntersVehicleEvent e = (PersonEntersVehicleEvent) event;
			if (e.getPersonId().toString().startsWith("pt_")){
				return false;
			}
			if (this.networkContainedLastLinkEnterByVehicleId.containsKey(e.getVehicleId()) 
					&& this.networkContainedLastLinkEnterByVehicleId.get(e.getVehicleId())){
				this.countedPersonIds.add(e.getPersonId());
				return true;
			}
		}
		else if (event instanceof PersonLeavesVehicleEvent){
			PersonLeavesVehicleEvent e = (PersonLeavesVehicleEvent) event;
			if (e.getPersonId().toString().startsWith("pt_")){
				return false;
			}
			if (this.networkContainedLastLinkEnterByVehicleId.containsKey(e.getVehicleId()) 
					&& this.networkContainedLastLinkEnterByVehicleId.get(e.getVehicleId()) 
					&& countedPersonIds.contains(e.getPersonId())){
				countedPersonIds.remove(e.getPersonId());
				return true;
			}
			else if (this.countedPersonIds.contains(e.getPersonId())){
				countedPersonIds.remove(e.getPersonId());
				return true;
			}
		}

		if (linkId != null){
			if (this.network.getLinks().containsKey(linkId)){
				this.networkContainedLastLinkEnterByVehicleId.put(vehId, true);
				return true;
			}
			else {
				this.networkContainedLastLinkEnterByVehicleId.put(vehId, false);
				return true;
			}
		}

		
		
		return false;
	}

	
	
	
	
}
