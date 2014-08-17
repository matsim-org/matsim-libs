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
package playground.agarwalamit.analysis.legModeHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

/**
 * Handles departure and arrival events and store total travel time of a person and 
 * travel time of each trip of a person segregated by leg mode
 * @author amit
 */
public class LegModeTravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private SortedMap<String, Map<Id, List<Double>>> mode2PersonId2TravelTimes;
	private Map<Id, Double> personId2DepartureTime;

	public LegModeTravelTimeHandler() {
		this.mode2PersonId2TravelTimes = new TreeMap<String, Map<Id,List<Double>>>();
		this.personId2DepartureTime = new HashMap<Id, Double>();
	}

	@Override
	public void reset(int iteration) {
		this.mode2PersonId2TravelTimes.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String legMode = event.getLegMode();
		Id personId = event.getPersonId();
		double arrivalTime = event.getTime();
		double travelTime = arrivalTime - this.personId2DepartureTime.get(personId);

		if(this.mode2PersonId2TravelTimes.containsKey(legMode)){
			Map<Id, List<Double>> personId2TravelTimes = this.mode2PersonId2TravelTimes.get(legMode);
			if(personId2TravelTimes.containsKey(personId)){
				List<Double> travelTimes = personId2TravelTimes.get(personId);
				travelTimes.add(travelTime);
				personId2TravelTimes.put(personId, travelTimes);
			} else {
				List<Double> travelTimes = new ArrayList<Double>();
				travelTimes.add(travelTime);
				personId2TravelTimes.put(personId, travelTimes);
			}
		} else { 
			Map<Id, List<Double>> personId2TravelTimes = new HashMap<Id, List<Double>>();
			List<Double> travelTimes = new ArrayList<Double>();
			travelTimes.add(travelTime);
			personId2TravelTimes.put(personId, travelTimes);
			this.mode2PersonId2TravelTimes.put(legMode, personId2TravelTimes);
		}
		this.personId2DepartureTime.remove(personId);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id personId = event.getPersonId();
		double deartureTime = event.getTime();
		this.personId2DepartureTime.put(personId, deartureTime);
	}

	public SortedMap<String, Map<Id, Double>> getLegMode2PersonId2TotalTravelTime(){
		SortedMap<String, Map<Id, Double>> mode2PersonId2TotalTravelTime = new TreeMap<String, Map<Id,Double>>();
		for(String mode:mode2PersonId2TravelTimes.keySet()){
			Map<Id, Double> personId2TotalTravelTime = new HashMap<Id, Double>();
			for(Id id:mode2PersonId2TravelTimes.get(mode).keySet()){
				double travelTime=0;
				for(double d:mode2PersonId2TravelTimes.get(mode).get(id)){
					travelTime += d;
				}
				personId2TotalTravelTime.put(id, travelTime);
			}
			mode2PersonId2TotalTravelTime.put(mode, personId2TotalTravelTime);
		}
		return mode2PersonId2TotalTravelTime;
	}

	public SortedMap<String, Map<Id, List<Double>>> getLegMode2PesonId2TripTimes (){
		return this.mode2PersonId2TravelTimes;
	}
}
