/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToFacilityOccupancy.java
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

package occupancy;

import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

public class EventsToFacilityOccupancy implements ActivityStartEventHandler, ActivityEndEventHandler {
	
	private TreeMap<Id, FacilityOccupancy> facilityOccupancies;
	
	@Override
	public void handleEvent(final ActivityStartEvent event) {
		if (!(event.getActType().startsWith("h") || event.getActType().startsWith("tta"))) {
			Id facilityId = event.getFacilityId();
			this.facilityOccupancies.get(facilityId).addArrival(event.getTime());
		}
	}
	
	@Override
	public void handleEvent(final ActivityEndEvent event) {
		if (!(event.getActType().startsWith("h") || event.getActType().startsWith("tta"))) {
			Id facilityId = event.getFacilityId();
			this.facilityOccupancies.get(facilityId).addDeparture(event.getTime());
		}
	}
	
	@Override
	public void reset(final int iteration) {
			}
	
	public TreeMap<Id, FacilityOccupancy> getFacilityOccupancies() {
		return facilityOccupancies;
	}
}
