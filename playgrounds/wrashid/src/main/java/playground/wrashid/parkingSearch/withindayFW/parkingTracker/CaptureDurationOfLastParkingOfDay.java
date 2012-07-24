/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.parkingTracker;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

import playground.wrashid.lib.GeneralLib;

//Done.
public class CaptureDurationOfLastParkingOfDay implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private Map<Id, Double> firstDepartureTimeOfDay = new HashMap<Id, Double>();
	private Map<Id, Double> lastArrivalTimeOfDay = new HashMap<Id, Double>();

	public Double getDuration(Id personId) {
		return GeneralLib.getIntervalDuration(lastArrivalTimeOfDay.get(personId), firstDepartureTimeOfDay.get(personId));
	}

	@Override
	public void reset(int iteration) {
		firstDepartureTimeOfDay.clear();
		lastArrivalTimeOfDay.clear();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			lastArrivalTimeOfDay.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (!firstDepartureTimeOfDay.containsKey(event.getPersonId())) {
			if (event.getLegMode().equals(TransportMode.car)) {
				firstDepartureTimeOfDay.put(event.getPersonId(), event.getTime());
			}
		}
	}

	public double getFirstDepartureTimeOfDay(Id personId) {
		return firstDepartureTimeOfDay.get(personId);
	}

}
