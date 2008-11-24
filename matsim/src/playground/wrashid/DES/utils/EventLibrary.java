package playground.wrashid.DES.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.PersonEvent;

import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;

public class EventLibrary {
	
	/*
	 * Get the travel time of one person. This means the time, between starting
	 * each leg end its end.
	 */
	public static double getTravelTime(LinkedList<PersonEvent> events, int agentId){
		double travelTime = 0;
		double startLegTime = 0;
		
		for (int i = 0; i < events.size(); i++) {
			if (Integer.parseInt(events.get(i).agentId)  == agentId) {
				if (events.get(i) instanceof AgentDepartureEvent) {
					startLegTime = events.get(i).time;
				} else if (events.get(i) instanceof AgentArrivalEvent) {
					travelTime += events.get(i).time - startLegTime;
				}
			}
		}

		return travelTime;
	}
	
	
	public static double getSumTravelTime(LinkedList<PersonEvent> events) {
		double travelTime = 0;
		
		// key=vehicleId, value=starting time of last leg
		HashMap<String, Double> startingTime = new HashMap<String, Double>();
		
		for (int i = 0; i < events.size(); i++) {
				if (events.get(i) instanceof AgentDepartureEvent) {
					startingTime.put(events.get(i).agentId, events.get(i).time);
				} else if (events.get(i) instanceof AgentArrivalEvent) {
					travelTime += events.get(i).time - startingTime.get(events.get(i).agentId);
				}
		}

		return travelTime;
	}
	
	
	
}
