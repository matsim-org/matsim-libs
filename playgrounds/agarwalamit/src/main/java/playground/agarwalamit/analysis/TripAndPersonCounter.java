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

import playground.agarwalamit.utils.MapUtils;

/**
 * @author amit
 */
public class TripAndPersonCounter implements PersonDepartureEventHandler, PersonArrivalEventHandler {
	private static final Logger LOGGER = Logger.getLogger(TripAndPersonCounter.class);
	private final Map<Id<Person>,Integer> personId2CarTripCounter;
	private final Map<Id<Person>,Integer> personId2AllTripCounter;
	private final List<Id<Person>> departureList;
	
	public TripAndPersonCounter() {
		this.personId2CarTripCounter = new HashMap<>();
		this.personId2AllTripCounter = new HashMap<>();
		this.departureList = new ArrayList<>();
	}

	@Override
	public void reset(int iteration) {
		this.personId2CarTripCounter.clear();
		this.personId2AllTripCounter.clear();
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
			if (this.personId2CarTripCounter.containsKey(event.getPersonId())) {
				int oldTrips = this.personId2CarTripCounter.get(event.getPersonId());
				int newTrips = oldTrips+1;
				this.personId2CarTripCounter.put(event.getPersonId(), newTrips);
			} else {
				this.personId2CarTripCounter.put(event.getPersonId(), 1);
			}
		}
		
		if (this.personId2AllTripCounter.containsKey(event.getPersonId())) {
			int oldTrips = this.personId2AllTripCounter.get(event.getPersonId());
			int newTrips = oldTrips+1;
			this.personId2AllTripCounter.put(event.getPersonId(), newTrips);
		} else {
			this.personId2AllTripCounter.put(event.getPersonId(), 1);
		}
		
	}
	
	public int getTotalNumberOfPersons(){
		checkPersonNotArrived();
		return this.personId2AllTripCounter.size();
	}
	
	public int getNumberOfCarPersons(){
		checkPersonNotArrived();
		return this.personId2CarTripCounter.size();
	}
	
	public int getTotalNumberOfTrips(){
		checkPersonNotArrived();
		return MapUtils.intValueSum(this.personId2AllTripCounter);
	}
	
	public int getNumberOfCarTrips(){
		checkPersonNotArrived();
		return MapUtils.intValueSum(this.personId2CarTripCounter);
	}
	
	public double getAverageCarTripPerCarPerson(){
		return (double) getNumberOfCarTrips() / (double) getNumberOfCarPersons();
	}
	
	private void checkPersonNotArrived(){
		if(!this.departureList.isEmpty()){
			LOGGER.warn(this.departureList.size() +" persons are not arrived to activites. Reasons could be \n "
					+ "1) Person is `StuckAndAbort' 2) Person is enroute at simulation end time.");
		}
	}
}