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
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

public class SfFlightTimeEventHandler implements AgentArrivalEventHandler,
		AgentDepartureEventHandler {
	
	public Map<Id, Double> arrivalTime;
	public Map<Id, Double> departureTime;
	public Map<Id, Double> flightTime;


	public SfFlightTimeEventHandler() {
		this.arrivalTime = new HashMap<Id, Double>();
		this.departureTime = new HashMap<Id, Double>();
		this.flightTime = new HashMap<Id, Double>();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.arrivalTime.put(event.getPersonId(), event.getTime());
		this.flightTime.put(event.getPersonId(), event.getTime()-this.departureTime.get(event.getPersonId()));
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.departureTime.put(event.getPersonId(), event.getTime());
	}
	
	public Map<Id, Double> returnArrival() {
		return this.arrivalTime;	
	}

	public Map<Id, Double> returnDeparture() {
		return this.arrivalTime;		
	}
	
	public Map<Id, Double> returnFlightTime() {
		return this.flightTime;		
	}
	
}
