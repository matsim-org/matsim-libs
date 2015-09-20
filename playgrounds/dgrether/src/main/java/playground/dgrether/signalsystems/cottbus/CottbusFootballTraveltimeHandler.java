/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusFootballTraveltimeHandler
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
package playground.dgrether.signalsystems.cottbus;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import playground.dgrether.signalsystems.cottbus.footballdemand.CottbusFootballStrings;

/**
 * @author dgrether
 * 
 */
public class CottbusFootballTraveltimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private Map<Id<Person>, Double> arrivaltimesSPN2FB;
	private Map<Id<Person>, Double> arrivaltimesCB2FB;
	private Map<Id<Person>, Double> arrivaltimesFB2SPN;
	private Map<Id<Person>, Double> arrivaltimesFB2CB;
	
	private Map<Id<Person>, Double> travelTimesPerPerson;

	public CottbusFootballTraveltimeHandler(){
		this.reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		this.travelTimesPerPerson = new TreeMap<>();
		this.arrivaltimesFB2CB = new TreeMap<>();
		this.arrivaltimesFB2SPN = new TreeMap<>();
		this.arrivaltimesCB2FB = new TreeMap<>();
		this.arrivaltimesSPN2FB = new TreeMap<>();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double previousTT = travelTimesPerPerson.get(event.getPersonId());
		travelTimesPerPerson.put(event.getPersonId(), previousTT + event.getTime());
		
		if (event.getPersonId().toString().endsWith(CottbusFootballStrings.SPN2FB)) {
			Double tr = this.arrivaltimesSPN2FB.get(event.getPersonId());
			if (tr == null) {
				this.arrivaltimesSPN2FB.put(event.getPersonId(), event.getTime());
			}
			else {
				this.arrivaltimesFB2SPN.put(event.getPersonId(), event.getTime());
			}
		}
		if (event.getPersonId().toString().endsWith(CottbusFootballStrings.CB2FB)) {
			Double tr = this.arrivaltimesCB2FB.get(event.getPersonId());
			if (tr == null) {
				this.arrivaltimesCB2FB.put(event.getPersonId(), event.getTime());
			}
			else {
				this.arrivaltimesFB2CB.put(event.getPersonId(), event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!travelTimesPerPerson.containsKey(event.getPersonId()))
			travelTimesPerPerson.put(event.getPersonId(), 0.0);
		double previousTT = travelTimesPerPerson.get(event.getPersonId());
		travelTimesPerPerson.put(event.getPersonId(), previousTT - event.getTime());
	}

	public double getAverageTravelTime() {
		Double att = 0.0;
		for (Double travelTime : travelTimesPerPerson.values()) {
			att += travelTime;
		}
		att = att / travelTimesPerPerson.size();
		return att;
	}

	public Map<Id<Person>, Double> getArrivalTimesCB2FB() {
		return this.arrivaltimesCB2FB;
	}

	public Map<Id<Person>, Double> getArrivalTimesFB2CB() {
		return this.arrivaltimesFB2CB;
	}

	public Map<Id<Person>, Double> getArrivalTimesSPN2FB() {
		return this.arrivaltimesSPN2FB;
	}

	public Map<Id<Person>, Double> getArrivalTimesFB2SPN() {
		return this.arrivaltimesFB2SPN;
	}

}
