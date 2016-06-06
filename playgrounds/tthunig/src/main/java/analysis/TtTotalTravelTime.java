/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * Sums up travel times of all agents, i.e. the time between person departure and arrival event
 * 
 * @author tthunig
 */
public class TtTotalTravelTime implements PersonDepartureEventHandler, PersonArrivalEventHandler{

	private double totalTt = 0.0;
	private Map<Id<Person>, Double> pers2lastDepatureTime = new HashMap<>();
	
	
	@Override
	public void reset(int iteration) {
		totalTt = 0.0;
		pers2lastDepatureTime.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double tripDuration = event.getTime() - pers2lastDepatureTime.get(event.getPersonId());
		totalTt += tripDuration;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		pers2lastDepatureTime.put(event.getPersonId(), event.getTime());
	}

	public double getTotalTt(){
		return totalTt;
	}
	
}
