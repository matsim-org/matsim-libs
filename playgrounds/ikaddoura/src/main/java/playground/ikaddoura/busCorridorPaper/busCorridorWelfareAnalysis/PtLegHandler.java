/* *********************************************************************** *
 * project: org.matsim.*
 * InVehWaitHandler.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

/**
 * @author Ihab
 *
 */
public class PtLegHandler implements PersonEntersVehicleEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler {
	private final Map <Id, Double> personId2WaitingTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2PersonEntersVehicleTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2AgentDepartureTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2InVehicleTime = new HashMap<Id, Double>();

	@Override
	public void reset(int iteration) {
		personId2WaitingTime.clear();
		personId2PersonEntersVehicleTime.clear();
		personId2AgentDepartureTime.clear();
		personId2InVehicleTime.clear();
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
		
		if (personId.toString().contains("person") && vehId.toString().contains("bus")){
			personId2PersonEntersVehicleTime.put(personId, event.getTime());
			
			double waitingTime;
			if (personId2AgentDepartureTime.get(personId) == null){
				throw new RuntimeException("Person " + personId + " is entering vehicle " + vehId + " without having departed from an activity. Aborting...");
			} else {
				waitingTime =  event.getTime() - personId2AgentDepartureTime.get(personId);
			}
			
			if (personId2WaitingTime.get(personId) == null){
				personId2WaitingTime.put(personId, waitingTime);
			} else {
				double waitingTimeSum = personId2WaitingTime.get(personId) + waitingTime;
				personId2WaitingTime.put(personId, waitingTimeSum);
			}
		} else {
			// no person enters a bus
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().toString().equals("pt")){
			personId2AgentDepartureTime.put(event.getPersonId(), event.getTime());
		}
	}

	// TODO: adjust as above...
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		
		if (event.getLegMode().toString().equals("pt")){
			double inVehicleTime = 0;
			if (personId2PersonEntersVehicleTime.containsKey(event.getPersonId())){
				inVehicleTime = event.getTime() - personId2PersonEntersVehicleTime.get(event.getPersonId());
			}
			else {
				System.out.println("Person kommt an ohne in ein Vehicle gestiegen zu sein!");
			}
			
			if (personId2InVehicleTime.containsKey(event.getPersonId())){
				double inVehicleTimeSum = personId2InVehicleTime.get(event.getPersonId()) + inVehicleTime;
				personId2InVehicleTime.put(event.getPersonId(), inVehicleTimeSum);
			}
			else {
				personId2InVehicleTime.put(event.getPersonId(), inVehicleTime);
			}
		}		
	}

	public Map<Id, Double> getPersonId2WaitingTime() {
		return personId2WaitingTime;
	}

	public Map<Id, Double> getPersonId2InVehicleTime() {
		return personId2InVehicleTime;
	}

	public double getSumOfWaitingTimes() {
		double sumOfWaitingTimes = 0.0;
		for(Id personId : personId2WaitingTime.keySet()){
			sumOfWaitingTimes = sumOfWaitingTimes + personId2WaitingTime.get(personId);
		}
		return sumOfWaitingTimes;
	}
}
