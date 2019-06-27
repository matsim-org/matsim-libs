/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToFacilityLoad.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.frozenepsilons;

import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

/**
 * @author anhorni
 * Uses FacilityPenalty to manage the facililities' loads by taking care of activity start and end events.
 */
class EventsToFacilityLoad implements ActivityStartEventHandler, ActivityEndEventHandler {

	private TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final static Logger log = Logger.getLogger(EventsToFacilityLoad.class);

	public EventsToFacilityLoad(final ActivityFacilities facilities, double scaleNumberOfPersons,
			TreeMap<Id, FacilityPenalty> facilityPenalties, DestinationChoiceConfigGroup config) {
		super();

		this.facilityPenalties = facilityPenalties;

		log.info("facilities size: " + facilities.getFacilities().values().size());
				
		int counter = 0;
		int nextMsg = 1;
		for (ActivityFacility f : facilities.getFacilities().values()) {
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" facility # " + counter);
			}
			double capacity = Double.MAX_VALUE;
			Iterator<? extends ActivityOption> iter_act = f.getActivityOptions().values().iterator();
			while (iter_act.hasNext()){
				ActivityOption act = iter_act.next();
				if (act.getCapacity() < capacity) {
					capacity = act.getCapacity();
				}
			}
			this.facilityPenalties.put(f.getId(), new FacilityPenalty(capacity, scaleNumberOfPersons, config));
		}
		log.info("finished init");
	}

	/**
	 * Add an arrival event in "FacilityLoad" for every start of an activity
	 * Home activities are excluded.
	 */
	@Override
	public void handleEvent(final ActivityStartEvent event) {
		if (!(event.getActType().startsWith("h") || event.getActType().startsWith("tta"))) {
			Id facilityId = event.getFacilityId();
			this.facilityPenalties.get(facilityId).getFacilityLoad().addArrival(event.getTime());
		}
	}

	/**
	 * Add a departure event in "FacilityLoad" for every ending of an activity
	 * Home activities are excluded
	 */
	@Override
	public void handleEvent(final ActivityEndEvent event) {
		if (!(event.getActType().startsWith("h") || event.getActType().startsWith("tta"))) {
			Id facilityId = event.getFacilityId();
			this.facilityPenalties.get(facilityId).getFacilityLoad().addDeparture(event.getTime());
		}
	}

	public void finish() {
		log.info("EventsToFacilityLoad start finish() method");
		Iterator<? extends FacilityPenalty> iter_fp = this.facilityPenalties.values().iterator();
		while (iter_fp.hasNext()){
			FacilityPenalty fp = iter_fp.next();
			fp.finish();
		}
		log.info("EventsToFacilityLoad end finish() method");
	}

	@Override
	public void reset(final int iteration) {
		log.info("Not really resetting anything here.");
	}

	public void resetAll(final int iteration) {
		Iterator<? extends FacilityPenalty> iter_fp = this.facilityPenalties.values().iterator();
		while (iter_fp.hasNext()){
			FacilityPenalty fp = iter_fp.next();
			fp.reset();
		}
		log.info("EventsToFacilityLoad resetted");
	}

	public TreeMap<Id, FacilityPenalty> getFacilityPenalties() {
		return facilityPenalties;
	}

//	public void setFacilityPenalties(TreeMap<Id, FacilityPenalty> facilityPenalties) {
//		this.facilityPenalties = facilityPenalties;
//	}
}
