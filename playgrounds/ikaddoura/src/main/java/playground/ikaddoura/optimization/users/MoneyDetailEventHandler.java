/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.optimization.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author Ihab
 *
 */
public class MoneyDetailEventHandler implements PersonMoneyEventHandler, ActivityEndEventHandler {
	
	List<Id<Person>> personIDsSecondTrip = new ArrayList<Id<Person>>();
	
	Map<Id<Person>, Double> personId2fareFirstTrip = new HashMap<Id<Person>, Double>();
	Map<Id<Person>, Double> personId2fareSecondTrip = new HashMap<Id<Person>, Double>();
	
	Map<Id<Person>, Double> personId2firstTripDepartureTime = new HashMap<Id<Person>, Double>();
	Map<Id<Person>, Double> personId2secondTripDepartureTime = new HashMap<Id<Person>, Double>();
	
	@Override
	public void reset(int iteration) {
		this.personIDsSecondTrip.clear();
		this.personId2fareFirstTrip.clear();
		this.personId2fareSecondTrip.clear();
		this.personId2firstTripDepartureTime.clear();
		this.personId2secondTripDepartureTime.clear();
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		
		if (this.personIDsSecondTrip.contains(event.getPersonId())){
			// second trip
			if (personId2fareSecondTrip.containsKey(event.getPersonId())){
				double amountSum = this.personId2fareSecondTrip.get(event.getPersonId()) + event.getAmount();
				this.personId2fareSecondTrip.put(event.getPersonId(), amountSum);
			} else {
				this.personId2fareSecondTrip.put(event.getPersonId(), event.getAmount());
			}
			
		} else {
			if (personId2fareFirstTrip.containsKey(event.getPersonId())){
				double amountSum = this.personId2fareFirstTrip.get(event.getPersonId()) + event.getAmount();
				this.personId2fareFirstTrip.put(event.getPersonId(), amountSum);
			} else {
				this.personId2fareFirstTrip.put(event.getPersonId(), event.getAmount());
			}
		}
	}
	
	public Map<Double, Double> getAvgFarePerTripDepartureTime() {
		Map<Double, Double> tripDepTime2avgFare = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDepTime2fares = new HashMap<Double, List<Double>>();
		double startTime = 4. * 3600;
		double periodLength = 7200;
		double endTime = 24. * 3600;
		
		for (double time = startTime; time <= endTime; time = time + periodLength){
			List<Double> fares = new ArrayList<Double>();
			tripDepTime2fares.put(time, fares);
		}
			
		for (Double time : tripDepTime2fares.keySet()){
			for (Id<Person> personId : this.personId2firstTripDepartureTime.keySet()){
				if (this.personId2firstTripDepartureTime.get(personId) < time && this.personId2firstTripDepartureTime.get(personId) >= (time - periodLength)) {
					if (tripDepTime2fares.containsKey(time)){
						tripDepTime2fares.get(time).add(this.personId2fareFirstTrip.get(personId));
					}
				}
			}
			
			for (Id<Person> personId : this.personId2secondTripDepartureTime.keySet()){
				if (this.personId2secondTripDepartureTime.get(personId) < time && this.personId2secondTripDepartureTime.get(personId) >= (time - periodLength)) {
					if (tripDepTime2fares.containsKey(time)){
						tripDepTime2fares.get(time).add(this.personId2fareSecondTrip.get(personId));
					}
				}
			}
		}
		
		for (Double time : tripDepTime2fares.keySet()){
			double fareSum = 0.;
			double counter = 0.;
			for (Double fare : tripDepTime2fares.get(time)){
				if (fare == null){
					
				} else {
					fareSum = fareSum + fare;
					counter++;
				}
			}
			
			double avgFare = 0.;
			if (counter!=0.){
				avgFare = (-1) * fareSum / counter;
			}
			tripDepTime2avgFare.put(time, avgFare);
		}
		return tripDepTime2avgFare;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equalsIgnoreCase("Home")){
			this.personId2firstTripDepartureTime.put(event.getPersonId(), event.getTime());
		} else if (event.getActType().equalsIgnoreCase("Other")){
			this.personIDsSecondTrip.add(event.getPersonId());
			this.personId2secondTripDepartureTime.put(event.getPersonId(), event.getTime());
		} else if (event.getActType().equalsIgnoreCase("Work")){
			this.personIDsSecondTrip.add(event.getPersonId());
			this.personId2secondTripDepartureTime.put(event.getPersonId(), event.getTime());
		}
	}

	public Map<Id<Person>, Double> getPersonId2fareFirstTrip() {
		return personId2fareFirstTrip;
	}

	public Map<Id<Person>, Double> getPersonId2fareSecondTrip() {
		return personId2fareSecondTrip;
	}
	
}
