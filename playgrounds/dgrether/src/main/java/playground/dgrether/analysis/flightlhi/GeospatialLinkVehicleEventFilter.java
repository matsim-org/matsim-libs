/* *********************************************************************** *
 * project: org.matsim.*
 * GeospatialLinkVehicleEventFilter
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
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;

import playground.dgrether.events.filters.EventFilter;


/**
 * @author dgrether
 */
public class GeospatialLinkVehicleEventFilter implements EventFilter {

	private Network network;
	private Map<Id, Boolean> networkContainedLastLinkEnterByVehicleId;
	private Map<Id, VehicleDepartsAtFacilityEvent> vehDepartsEventsByVehicleId;
	
	/**
	 * @param network
	 */
	public GeospatialLinkVehicleEventFilter(Network network) {
		this.network =network;
		this.networkContainedLastLinkEnterByVehicleId = new HashMap<Id, Boolean>();
		this.vehDepartsEventsByVehicleId  = new HashMap<Id, VehicleDepartsAtFacilityEvent>();
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
		else if (event instanceof VehicleDepartsAtFacilityEvent){
			VehicleDepartsAtFacilityEvent e = (VehicleDepartsAtFacilityEvent) event;
			if (this.networkContainedLastLinkEnterByVehicleId.containsKey(e.getVehicleId()) 
					&& this.networkContainedLastLinkEnterByVehicleId.get(e.getVehicleId())){
				
				this.vehDepartsEventsByVehicleId.put(e.getVehicleId(), e);
				return true;
			}
		}
		else if (event instanceof VehicleArrivesAtFacilityEvent) {
			VehicleArrivesAtFacilityEvent e = (VehicleArrivesAtFacilityEvent) event;
			if (this.networkContainedLastLinkEnterByVehicleId.containsKey(e.getVehicleId()) 
					&& this.networkContainedLastLinkEnterByVehicleId.get(e.getVehicleId()) 
					&& this.vehDepartsEventsByVehicleId.containsKey(e.getVehicleId())){
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
