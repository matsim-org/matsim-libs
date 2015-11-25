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
package playground.agarwalamit.analysis.trip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;

/**
 * Handles departure and arrival events and store total travel time of a person and 
 * travel time of each trip of a person segregated by leg mode
 * @author amit
 */

public class LegModeTripTravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

	private final Logger logger = Logger.getLogger(LegModeTripTravelTimeHandler.class);
	private SortedMap<String, Map<Id<Person>, List<Double>>> mode2PersonId2TravelTimes;
	private Map<Id<Person>, Double> personId2DepartureTime;
	private final int maxStuckAndAbortWarnCount=5;
	private int warnCount = 0;
	private Set<Id<Person>> stuckPersons;
	private SortedMap<String, Double> mode2NumberOfLegs ;

	public LegModeTripTravelTimeHandler() {
		this.mode2PersonId2TravelTimes = new TreeMap<String, Map<Id<Person>,List<Double>>>();
		this.personId2DepartureTime = new HashMap<Id<Person>, Double>();
		this.stuckPersons = new HashSet<Id<Person>>();
		this.mode2NumberOfLegs = new TreeMap<String, Double>();
	}

	@Override
	public void reset(int iteration) {
		this.mode2PersonId2TravelTimes.clear();
		this.personId2DepartureTime.clear();
		this.stuckPersons.clear();
		this.mode2NumberOfLegs.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String legMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();
		double arrivalTime = event.getTime();
		double travelTime = arrivalTime - this.personId2DepartureTime.get(personId);

		if(this.mode2PersonId2TravelTimes.containsKey(legMode)){
			Map<Id<Person>, List<Double>> personId2TravelTimes = this.mode2PersonId2TravelTimes.get(legMode);
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
			Map<Id<Person>, List<Double>> personId2TravelTimes = new HashMap<Id<Person>, List<Double>>();
			List<Double> travelTimes = new ArrayList<Double>();
			travelTimes.add(travelTime);
			personId2TravelTimes.put(personId, travelTimes);
			this.mode2PersonId2TravelTimes.put(legMode, personId2TravelTimes);
		}
		this.personId2DepartureTime.remove(personId);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Person> personId = event.getPersonId();
		double deartureTime = event.getTime();
		this.personId2DepartureTime.put(personId, deartureTime);
	}
	
	/**
	 * @return  Total travel time (summed for all trips for that person) for each person segregated w.r.t. travel modes.
	 */
	public SortedMap<String, Map<Id<Person>, Double>> getLegMode2PersonId2TotalTravelTime(){
		SortedMap<String, Map<Id<Person>, Double>> mode2PersonId2TotalTravelTime = new TreeMap<String, Map<Id<Person>,Double>>();
		for(String mode:this.mode2PersonId2TravelTimes.keySet()){
			double noOfLeg =0;
			Map<Id<Person>, Double> personId2TotalTravelTime = new HashMap<Id<Person>, Double>();
			for(Id<Person> id:this.mode2PersonId2TravelTimes.get(mode).keySet()){
				double travelTime=0;
				for(double d:this.mode2PersonId2TravelTimes.get(mode).get(id)){
					travelTime += d;
					noOfLeg++;
				}
				personId2TotalTravelTime.put(id, travelTime);
			}
			mode2PersonId2TotalTravelTime.put(mode, personId2TotalTravelTime);
			this.mode2NumberOfLegs.put(mode, noOfLeg);
		}
		return mode2PersonId2TotalTravelTime;
	}

	/**
	 * @return  trip time for each trip of each person segregated w.r.t. travel modes.
	 */
	public SortedMap<String, Map<Id<Person>, List<Double>>> getLegMode2PesonId2TripTimes (){
		return this.mode2PersonId2TravelTimes;
	}
	
	public Set<Id<Person>> getStuckPersonsList (){
		return stuckPersons;
	}
	
	public SortedMap<String,Double> getTravelMode2NumberOfLegs(){
		getLegMode2PersonId2TotalTravelTime();
		return this.mode2NumberOfLegs;
	}
	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.stuckPersons.add(event.getPersonId());
		this.warnCount++;
		if(this.warnCount<=this.maxStuckAndAbortWarnCount){
		this.logger.warn("'StuckAndAbort' event is thrown for person "+event.getPersonId()+" on link "+event.getLinkId()+" at time "+event.getTime()+
				". \n Correctness of travel time for such persons can not be guaranteed.");
		if(this.warnCount==this.maxStuckAndAbortWarnCount) this.logger.warn(Gbl.FUTURE_SUPPRESSED);
		}
	}
}