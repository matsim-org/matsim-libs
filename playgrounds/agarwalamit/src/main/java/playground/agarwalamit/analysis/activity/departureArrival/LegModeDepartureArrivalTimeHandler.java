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
package playground.agarwalamit.analysis.activity.departureArrival;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 * 
 * need to be tested first.
 */
public class LegModeDepartureArrivalTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final Map<String, Map<Id<Person>, List<Double>>> mode2PersonId2ArrivalTime = new HashMap<>();
	private final Map<String, Map<Id<Person>, List<Double>>> mode2PersonId2DepartureTime = new HashMap<>();

	@Override
	public void reset(int iteration) {
		this.mode2PersonId2ArrivalTime.clear();
		this.mode2PersonId2DepartureTime.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String legMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();
		double departureTime =event.getTime();

		if(this.mode2PersonId2DepartureTime.containsKey(legMode)){
			Map<Id<Person>, List<Double>> personId2DepartureTime = this.mode2PersonId2DepartureTime.get(legMode);
			if(personId2DepartureTime.containsKey(personId)){
				List<Double> departureTimes  = personId2DepartureTime.get(personId);
				departureTimes.add(departureTime);
			} else {
				List<Double> departureTimes = new ArrayList<>();
				departureTimes.add(departureTime);
				personId2DepartureTime.put(personId, departureTimes );
			}
		} else {
			Map<Id<Person>, List<Double> > personId2DepartureTime = new HashMap<>();
			List<Double>   departureTimes  = new ArrayList<>();
			departureTimes.add(departureTime);
			personId2DepartureTime.put(personId, departureTimes);
			this.mode2PersonId2DepartureTime.put(legMode, personId2DepartureTime);
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String legMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();
		double arrivalTime =event.getTime();

		if(this.mode2PersonId2ArrivalTime.containsKey(legMode)){
			Map<Id<Person>, List<Double>> personId2ArrivalTime = this.mode2PersonId2ArrivalTime.get(legMode);
			if(personId2ArrivalTime.containsKey(personId)){
				List<Double> arrivalTimes = personId2ArrivalTime.get(personId);
				arrivalTimes.add(arrivalTime);
			} else {
				List<Double> arrivalTimes  = new ArrayList<>();
				arrivalTimes.add(arrivalTime);
				personId2ArrivalTime.put(personId,arrivalTimes );
			}
		} else {
			Map<Id<Person>, List<Double> > personId2ArrivalTime = new HashMap<>();
			List<Double> arrivalTimes  = new ArrayList<>();
			arrivalTimes.add(arrivalTime);
			personId2ArrivalTime.put(personId, arrivalTimes);
			this.mode2PersonId2ArrivalTime.put(legMode, personId2ArrivalTime);
		}
	}

	public Map<String, Map<Id<Person>, List<Double> >> getLegMode2PersonId2DepartureTime(){
		return this.mode2PersonId2DepartureTime;
	}
	public Map<String, Map<Id<Person>, List<Double> >> getLegMode2PersonId2ArrivalTime(){
		return this.mode2PersonId2ArrivalTime;
	}
}