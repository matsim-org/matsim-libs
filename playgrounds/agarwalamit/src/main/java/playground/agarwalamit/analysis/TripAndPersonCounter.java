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
package playground.agarwalamit.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 */
public class TripAndPersonCounter implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	public TripAndPersonCounter() {
		this.personId2TripCounter = new HashMap<Id<Person>, Integer>();
		this.departureList = new ArrayList<Id<Person>>();
	}

	private Map<Id<Person>,Integer> personId2TripCounter;
	private List<Id<Person>> departureList;
	private final Logger logger = Logger.getLogger(TripAndPersonCounter.class);

	@Override
	public void reset(int iteration) {
		this.personId2TripCounter.clear();
		this.departureList.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.departureList.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.departureList.add(event.getPersonId());
		if(event.getLegMode().equals(TransportMode.car)){
			if (this.personId2TripCounter.containsKey(event.getPersonId())) {
				int oldTrips = this.personId2TripCounter.get(event.getPersonId());
				int newTrips = oldTrips+1;
				this.personId2TripCounter.put(event.getPersonId(), newTrips);
			} else {
				this.personId2TripCounter.put(event.getPersonId(), 1);
			}
		}
	}
	
	public int getNumberOfPersons(){
		checkPersonNotArrived();
		return this.personId2TripCounter.size();
	}
	
	public int getNumberOfTrips(){
		checkPersonNotArrived();
		int sumTrips =0;
		for(Id<Person> id:this.personId2TripCounter.keySet()){
			sumTrips += this.personId2TripCounter.get(id);
		}
		return sumTrips;
	}
	
	public double getAverageTripsPerPerson(){
		return ((double) getNumberOfTrips() / (double) getNumberOfPersons());
	}
	
	private void checkPersonNotArrived(){
		if(this.departureList.size()>0){
			logger.warn(this.departureList.size() +" persons are not arrived to activites. Reasons could be \n "
					+ "1) Person is `StuckAndAbort' 2) Person is enroute at simulation end time.");
		}
	}
}
