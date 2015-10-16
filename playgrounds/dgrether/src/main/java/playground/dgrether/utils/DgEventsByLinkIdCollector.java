/* *********************************************************************** *
 * project: org.matsim.*
 * EventsByLinkIdCollector
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class DgEventsByLinkIdCollector implements LinkLeaveEventHandler {
	private Map<Id<Vehicle>, Map<Id<Link>, LinkLeaveEvent>> events = new HashMap<>();
	private Set<Id<Link>> linkIds;
	
	public DgEventsByLinkIdCollector(Set<Id<Link>> linkIds) {
		this.linkIds  = linkIds;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.linkIds.contains(event.getLinkId())){
			Map<Id<Link>, LinkLeaveEvent> map = this.events.get(event.getVehicleId());
			if (map == null){
				map = new HashMap<Id<Link>, LinkLeaveEvent>();
				events.put(event.getVehicleId(), map);
			}
			map.put(event.getLinkId(), event);
		}
	}

	public LinkLeaveEvent getLinkLeaveEvent(Id<Vehicle> vehicleId, Id<Link> linkId){
		Map<Id<Link>, LinkLeaveEvent> m = this.events.get(vehicleId);
		if (m == null){
			throw new IllegalArgumentException("Cannot find link leave events for vehicle " + vehicleId);
		}
		return m.get(linkId);
	}
	
	@Override
	public void reset(int iteration) {
		this.events.clear();
	}

}