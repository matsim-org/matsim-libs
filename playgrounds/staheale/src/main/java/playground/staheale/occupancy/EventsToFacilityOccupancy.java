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

package playground.staheale.occupancy;

import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;


public class EventsToFacilityOccupancy implements ActivityStartEventHandler, ActivityEndEventHandler {

	private TreeMap<Id, FacilityOccupancy> facilityOccupancies;
	private final static Logger log = Logger.getLogger(EventsToFacilityOccupancy.class);


	public EventsToFacilityOccupancy(final ActivityFacilities facilities, 
			int numberOfTimeBins, double scaleNumberOfPersons,
			TreeMap<Id, FacilityOccupancy> facilityOccupancies, 
			DestinationChoiceConfigGroup config) {
		super();

		this.facilityOccupancies = facilityOccupancies;

		log.info("facilities size: " + facilities.getFacilities().values().size());

		int counter = 0;
		int nextMsg = 1;
		for (ActivityFacility f : facilities.getFacilities().values()) {
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" facility # " + counter);
			}
			this.facilityOccupancies.put(f.getId(), new FacilityOccupancy(numberOfTimeBins, scaleNumberOfPersons));
		}
		log.info("finished init");
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		if (!(event.getActType().startsWith("h")||
				event.getActType().startsWith("w") ||
				event.getActType().startsWith("tta") ||
				event.getActType().startsWith("e"))) {
			Id facilityId = event.getFacilityId();
			this.facilityOccupancies.get(facilityId).addArrival(event.getTime());
		}
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {
		if (!(event.getActType().startsWith("h")||
				event.getActType().startsWith("w") ||
				event.getActType().startsWith("tta") ||
				event.getActType().startsWith("e"))) {
			Id facilityId = event.getFacilityId();
			this.facilityOccupancies.get(facilityId).addDeparture(event.getTime());
		}
	}

	@Override
	public void reset(final int iteration) {
		Iterator<? extends FacilityOccupancy> iter_fo = this.facilityOccupancies.values().iterator();
		while (iter_fo.hasNext()){
			FacilityOccupancy fo = iter_fo.next();
			fo.reset();
		}
		log.info("EventsToFacilityOccupancy resetted");
	}

	public TreeMap<Id, FacilityOccupancy> getFacilityOccupancies() {
		return facilityOccupancies;
	}
}
