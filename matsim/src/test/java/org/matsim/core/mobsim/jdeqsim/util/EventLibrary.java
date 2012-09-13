package org.matsim.core.mobsim.jdeqsim.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;

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
			if (events.get(i) instanceof AgentDepartureEvent) {
				if (Integer.parseInt(((AgentDepartureEvent) events.get(i))
						.getPersonId().toString()) == agentId) {
					startLegTime = events.get(i).getTime();
				}
			} else if (events.get(i) instanceof AgentArrivalEvent) {
				if (Integer.parseInt(((AgentArrivalEvent) events.get(i))
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
			if (currentEvent instanceof AgentDepartureEvent) {
				if (currentEvent.getTime() < 0) {
					// the problem is, that some agent departure events are
					// negative.
					// this solves this problem
					startingTime
							.put(((AgentDepartureEvent) currentEvent).getPersonId().toString(), 0.0);
				} else {
					startingTime.put(((AgentDepartureEvent) currentEvent).getPersonId().toString(),
							currentEvent.getTime());
				}
			} else if (currentEvent instanceof AgentArrivalEvent) {
				travelTime += currentEvent.getTime()
						- startingTime.get(((AgentArrivalEvent) currentEvent).getPersonId()
								.toString());
			}
		}
		return travelTime;
	}

}
