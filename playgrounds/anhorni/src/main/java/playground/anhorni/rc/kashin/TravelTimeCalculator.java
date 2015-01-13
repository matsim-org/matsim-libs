/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                          *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.anhorni.rc.kashin;

import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;


public class TravelTimeCalculator implements  PersonDepartureEventHandler, PersonArrivalEventHandler {
	
	private TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private double overallTravelTime = 0.0;

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		// TODO: are events actually ordered by time?
		double traveltime = event.getTime() - this.agentDepartures.get(event.getPersonId());
		this.overallTravelTime += traveltime;		
	}

	@Override
	public void reset(int iteration) {
		this.agentDepartures.clear();
	}

	public double getOverallTravelTime() {
		return overallTravelTime;
	}
}
