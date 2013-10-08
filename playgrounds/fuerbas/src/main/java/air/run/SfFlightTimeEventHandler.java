/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
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

package air.run;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

public class SfFlightTimeEventHandler implements PersonArrivalEventHandler,
		PersonDepartureEventHandler {
	
	public Map<Id, Double> arrivalTime;
	public Map<Id, Double> departureTime;
	public Map<Id, Double> flightTime;


	public SfFlightTimeEventHandler() {
		this.arrivalTime = new HashMap<Id, Double>();
		this.departureTime = new HashMap<Id, Double>();
		this.flightTime = new HashMap<Id, Double>();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.arrivalTime.put(event.getPersonId(), event.getTime());
		this.flightTime.put(event.getPersonId(), event.getTime()-this.departureTime.get(event.getPersonId()));
	}


	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.departureTime.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void reset(int iteration) {
		this.arrivalTime.clear();
		this.departureTime.clear();
		this.flightTime.clear();
	}

	public Map<Id, Double> returnArrival() {
		return this.arrivalTime;	
	}

	public Map<Id, Double> returnDeparture() {
		return this.departureTime;		
	}
	
	public Map<Id, Double> returnFlightTime() {
		return this.flightTime;		
	}
	
}
