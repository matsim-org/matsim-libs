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
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.Event;

import playground.dgrether.events.filters.EventFilter;


/**
 * @author dgrether
 *
 */
public class GeospatialLinkDepartureArrivalStuckEventFilter implements EventFilter {

	private Network network;
	private Map<Id, Boolean> networkContainedLastDepartureByPersonId;

	/**
	 * @param network
	 */
	public GeospatialLinkDepartureArrivalStuckEventFilter(Network network) {
		this.network =network;
		this.networkContainedLastDepartureByPersonId = new HashMap<Id, Boolean>();
	}


	@Override
	public boolean doProcessEvent(Event event) {
		if (event instanceof AgentDepartureEvent){
			AgentDepartureEvent e = (AgentDepartureEvent) event;
			if (this.network.getLinks().containsKey(e.getLinkId())){
				this.networkContainedLastDepartureByPersonId.put(e.getPersonId(), true);
				return true;
			}
			else {
				this.networkContainedLastDepartureByPersonId.put(e.getPersonId(), false);
				return false;
			}
		}
		else if (event instanceof AgentArrivalEvent) {
			AgentArrivalEvent e = (AgentArrivalEvent) event;
			if (this.networkContainedLastDepartureByPersonId.get(e.getPersonId())
					&& this.network.getLinks().containsKey(e.getLinkId())) {
				return true;
			}
		}
		else if (event instanceof AgentStuckEvent){
			AgentStuckEvent e = (AgentStuckEvent) event;
			if (this.networkContainedLastDepartureByPersonId.get(e.getPersonId())
					&& this.network.getLinks().containsKey(e.getLinkId())) {
				return true;
			}
		}
		return false;
	}
	
}

