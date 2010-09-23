/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesOccupancyCounter.java
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

package playground.yu.locationChoice;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

/**
 * measures the occupancy degree of facilities from eventsfile
 * 
 * @author yu
 * 
 */
public class FacilitiesOccupancyMeasurement implements
		ActivityStartEventHandler, ActivityEndEventHandler {
	/**
	 * Map<facilityId, Map<time,number of agents in this facility"Id" at this
	 * time>> counts
	 */
	private Map<Id, Map<Double, Integer>> facilitiesDiaries = new HashMap<Id, Map<Double, Integer>>();

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!event.getActType().startsWith("h")) {
			Id facilityId = event.getFacilityId();
			Map<Double, Integer> diary = new HashMap<Double, Integer>();
		}
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// TODO Auto-generated method stub

	}

}
