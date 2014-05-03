/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.siouxFalls.legModeDistributions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

/**
 * @author amit
 */
public class LegModeTravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final Logger logger = Logger.getLogger(LegModeTravelTimeHandler.class);
	private Map<String, Map<Id, Double>> mode2PersonId2TravelTime;

	public LegModeTravelTimeHandler() {
		this.mode2PersonId2TravelTime = new HashMap<String, Map<Id,Double>>();
	}

	@Override
	public void reset(int iteration) {
		this.mode2PersonId2TravelTime.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String legMode = event.getLegMode();
		Id personId = event.getPersonId();
		double arrivalTime =event.getTime();

		if(this.mode2PersonId2TravelTime.containsKey(legMode)){
			Map<Id, Double> personId2TravelTime = this.mode2PersonId2TravelTime.get(legMode);
			if(personId2TravelTime.containsKey(personId)){
				double travelTimeSoFar = personId2TravelTime.get(personId);
				double newTravelTime = travelTimeSoFar+arrivalTime;
				personId2TravelTime.put(personId, newTravelTime);
			} else {
				personId2TravelTime.put(personId, arrivalTime);
			}
		} else {
			Map<Id, Double> personId2TravelTime = new HashMap<Id, Double>();
			personId2TravelTime.put(personId, arrivalTime);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String legMode = event.getLegMode();
		Id personId = event.getPersonId();
		double deartureTime =event.getTime();

		if(this.mode2PersonId2TravelTime.containsKey(legMode)){
			Map<Id, Double> personId2TravelTime = this.mode2PersonId2TravelTime.get(legMode);
			if(personId2TravelTime.containsKey(personId)){
				double travelTimeSoFar = personId2TravelTime.get(personId);
				double newTravelTime = travelTimeSoFar-deartureTime;
				personId2TravelTime.put(personId, newTravelTime);
			} else {
				personId2TravelTime.put(personId, -deartureTime);
			}
		} else {
			Map<Id, Double> personId2TravelTime = new HashMap<Id, Double>();
			personId2TravelTime.put(personId, -deartureTime);
			this.mode2PersonId2TravelTime.put(legMode, personId2TravelTime);
		}
	}

	public Map<String, Map<Id, Double>> getLegMode2PersonId2TravelTime(){
		return this.mode2PersonId2TravelTime;
	}
}
