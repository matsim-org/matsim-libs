
/* *********************************************************************** *
 * project: org.matsim.*
 * EventLibrary.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.jdeqsim.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;

/**
 * This class is used to calculate statistical values (used among others in some
 * tests).
 * 
 * @author rashid_waraich
 */
public class EventLibrary {

	/*
	 * Get the travel time of one person. This means the time, between starting
	 * each leg end its end.
	 */
	public static double getTravelTime(LinkedList<Event> events, int agentId) {
		double travelTime = 0;
		double startLegTime = 0;

		for (int i = 0; i < events.size(); i++) {
			if (events.get(i) instanceof PersonDepartureEvent) {
				if (Integer.parseInt(((PersonDepartureEvent) events.get(i))
						.getPersonId().toString()) == agentId) {
					startLegTime = events.get(i).getTime();
				}
			} else if (events.get(i) instanceof PersonArrivalEvent) {
				if (Integer.parseInt(((PersonArrivalEvent) events.get(i))
						.getPersonId().toString()) == agentId) {
					travelTime += events.get(i).getTime() - startLegTime;
				}
			}
		}

		return travelTime;
	}

	/*
	 * Get the sum of the travel time of all agents (time between each departure
	 * and arrival)
	 */
	public static double getSumTravelTime(List<? extends Event> events) {
		double travelTime = 0;

		// key=vehicleId, value=starting time of last leg
		HashMap<String, Double> startingTime = new HashMap<String, Double>();
		Event currentEvent = null;
		for (int i = 0; i < events.size(); i++) {
			currentEvent = events.get(i);
			if (currentEvent instanceof PersonDepartureEvent) {
				if (currentEvent.getTime() < 0) {
					// the problem is, that some agent departure events are
					// negative.
					// this solves this problem
					startingTime
							.put(((PersonDepartureEvent) currentEvent).getPersonId().toString(), 0.0);
				} else {
					startingTime.put(((PersonDepartureEvent) currentEvent).getPersonId().toString(),
							currentEvent.getTime());
				}
			} else if (currentEvent instanceof PersonArrivalEvent) {
				travelTime += currentEvent.getTime()
						- startingTime.get(((PersonArrivalEvent) currentEvent).getPersonId()
								.toString());
			}
		}
		return travelTime;
	}

}
