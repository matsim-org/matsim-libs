/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToScore.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice;

import java.util.Iterator;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;

/**
 *
 * @author anhorni
 */
public class EventsToFacilityLoad implements EventHandlerAgentArrivalI, EventHandlerAgentDepartureI {

	private Facilities facilities = null;

	public EventsToFacilityLoad(final Facilities facilities) {
		super();
		this.facilities = facilities;
	}

	public void handleEvent(final EventAgentDeparture event) {
		Facility facility=(Facility)event.link.getUpLocation(new IdImpl(event.linkId));
		facility.addArrival(event.time);
	}

	public void handleEvent(final EventAgentArrival event) {
		Facility facility=(Facility)event.link.getUpLocation(new IdImpl(event.linkId));
		facility.addDeparture(event.time);
	}

	public void finish() {
		Iterator<? extends Facility> iter = this.facilities.getFacilities().values().iterator();
		while (iter.hasNext()){
			Facility f = iter.next();
			f.calculateFacilityLoad24();
		}
	}

	public void reset(final int iteration) {
		Iterator<? extends Facility> iter = this.facilities.getFacilities().values().iterator();
		while (iter.hasNext()){
			Facility f = iter.next();
			f.reset();
		}
	}
}
