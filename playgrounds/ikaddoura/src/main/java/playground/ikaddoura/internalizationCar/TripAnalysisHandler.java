/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.internalizationCar;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;

/**
 * @author ikaddoura
 *
 */
public class TripAnalysisHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler{

	private Map<Id, Double> personId2departureTime = new HashMap<Id, Double>();
	private double travelTimeSum = 0.;
	private int agentStuckEvents = 0;
	
	@Override
	public void reset(int iteration) {
		this.personId2departureTime.clear();
		this.travelTimeSum = 0.;
		this.agentStuckEvents = 0;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double travelTime = event.getTime() - this.personId2departureTime.get(event.getPersonId());
		travelTimeSum = travelTimeSum + travelTime;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personId2departureTime.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		agentStuckEvents++;
	}

	public double getTotalTravelTime() {
		return travelTimeSum;
	}

	public int getAgentStuckEvents() {
		return agentStuckEvents;
	}
	
}
