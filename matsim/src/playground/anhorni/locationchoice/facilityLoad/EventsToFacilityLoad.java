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

package playground.anhorni.locationchoice.facilityLoad;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.events.EventActivityEnd;
import org.matsim.events.EventActivityStart;
import org.matsim.events.handler.EventHandlerActivityEndI;
import org.matsim.events.handler.EventHandlerActivityStartI;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;

/**
 *
 * @author anhorni
 */
public class EventsToFacilityLoad implements EventHandlerActivityStartI, EventHandlerActivityEndI {

	private Facilities facilities = null;
	private int scaleNumberOfPersons = 1;

	private final static Logger log = Logger.getLogger(EventsToFacilityLoad.class);

	public EventsToFacilityLoad(final Facilities facilities, int scaleNumberOfPersons) {
		super();
		this.facilities = facilities;
		this.scaleNumberOfPersons = scaleNumberOfPersons;
	}

	public void handleEvent(final EventActivityStart event) {
		Facility facility = event.act.getFacility();
		if (event.acttype.startsWith("s") || event.acttype.startsWith("l")) {
			facility.addArrival(event.time, this.scaleNumberOfPersons);
		}
	}

	public void handleEvent(final EventActivityEnd event) {
		Facility facility = event.act.getFacility();
		if (event.acttype.startsWith("s") || event.acttype.startsWith("l")) {
			facility.addDeparture(event.time);
		}
	}

	public void finish() {
		Iterator<? extends Facility> iter = this.facilities.getFacilities().values().iterator();
		while (iter.hasNext()){
			Facility f = iter.next();
			f.setScaleNumberOfPersons(this.scaleNumberOfPersons);
			f.finish();
		}
		log.info("EventsToFacilityLoad finished");
	}


	public void reset(final int iteration) {
		// 
	}
	
	public void resetAll(final int iteration) {
		Iterator<? extends Facility> iter = this.facilities.getFacilities().values().iterator();
		while (iter.hasNext()){
			Facility f = iter.next();
			f.reset();
		}
	}
}
