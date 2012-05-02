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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.pt.PtConstants;

/**
 * @author Ihab
 *
 */
public class PtLegHandler implements PersonEntersVehicleEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, ActivityEndEventHandler {
	private final Map <Id, Double> personId2WaitingTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2PersonEntersVehicleTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2AgentDepartureTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2InVehicleTime = new HashMap<Id, Double>();
	private boolean isEgress = false;
	
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
		Id personId = event.getPersonId();
		
		if (event.getLegMode().toString().equals("pt")){
			personId2AgentDepartureTime.put(personId, event.getTime());
		} else {
			// not a pt Leg
		}
		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();
		
		if (event.getLegMode().toString().equals("pt")){
			double inVehicleTime = 0.0;
			if (personId2PersonEntersVehicleTime.get(personId) == null){
				throw new RuntimeException("Person " + personId + " is arriving without having departed from an activity. Aborting...");
			} else {
				inVehicleTime = event.getTime() - personId2PersonEntersVehicleTime.get(personId);
			}
			
			if (personId2InVehicleTime.get(personId) == null) {
				personId2InVehicleTime.put(personId, inVehicleTime);
			} else {
				double inVehicleTimeSum = personId2InVehicleTime.get(personId) + inVehicleTime;
				personId2InVehicleTime.put(personId, inVehicleTimeSum);
			}			
			
		} else {
			// not a pt Leg
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

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().toString().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			this.isEgress = true;
		}
	}

	public boolean isEgress() {
		return isEgress;
	}

	public void setEgress(boolean isEgress) {
		this.isEgress = isEgress;
	}

}
