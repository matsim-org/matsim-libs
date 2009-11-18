package org.matsim.core.mobsim.jdeqsim.util;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;

/**
 * This class is used to calculate statistical values (used among others in some tests).
 *
 * @author rashid_waraich
 */
public class EventLibrary {

	/*
	 * Get the travel time of one person. This means the time, between starting
	 * each leg end its end.
	 */
	public static double getTravelTime(LinkedList<PersonEvent> events,
			int agentId) {
		double travelTime = 0;
		double startLegTime = 0;

		for (int i = 0; i < events.size(); i++) {
			if (Integer.parseInt(events.get(i).getPersonId().toString()) == agentId) {
				if (events.get(i) instanceof AgentDepartureEventImpl) {
					startLegTime = events.get(i).getTime();
				} else if (events.get(i) instanceof AgentArrivalEventImpl) {
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
	public static double getSumTravelTime(LinkedList<PersonEvent> events) {
		double travelTime = 0;

		// key=vehicleId, value=starting time of last leg
		HashMap<String, Double> startingTime = new HashMap<String, Double>();
		PersonEvent currentEvent = null;
		for (int i = 0; i < events.size(); i++) {
			currentEvent = events.get(i);
			if (currentEvent instanceof AgentDepartureEventImpl) {
				if (currentEvent.getTime() < 0) {
					// the problem is, that some agent departure events are
					// negative.
					// this solves this problem
					startingTime.put(currentEvent.getPersonId().toString(), 0.0);
				} else {
					startingTime.put(currentEvent.getPersonId().toString(), currentEvent.getTime());
				}
			} else if (currentEvent instanceof AgentArrivalEventImpl) {
				travelTime += currentEvent.getTime()
						- startingTime.get(currentEvent.getPersonId().toString());
			}
		}
		return travelTime;
	}

}
