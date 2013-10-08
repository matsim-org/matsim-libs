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
package playground.ikaddoura.busCorridor.busCorridorWelfareAnalysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;

/**
 * @author Ihab
 *
 */
public class PtLegHandler implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
	private Map <Id, Double> personId2WaitingTime = new HashMap<Id, Double>();
	private Map <Id, Double> personId2PersonEntersVehicleTime = new HashMap<Id, Double>();
	private Map <Id, Double> personId2AgentDepartureTime = new HashMap<Id, Double>();
	private Map <Id, Double> personId2InVehicleTime = new HashMap<Id, Double>();

	@Override
	public void reset(int iteration) {
		personId2WaitingTime.clear();
		personId2PersonEntersVehicleTime.clear();
		personId2AgentDepartureTime.clear();
		personId2InVehicleTime.clear();
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (event.getPersonId().toString().contains("person") && event.getVehicleId().toString().contains("bus")){

			personId2PersonEntersVehicleTime.put(event.getPersonId(), event.getTime());
			
			double waitingTime = 0;
			if (personId2AgentDepartureTime.containsKey(event.getPersonId())){
				waitingTime =  event.getTime() - personId2AgentDepartureTime.get(event.getPersonId());
			}
			else {
				System.out.println("Person steigt in Vehicle ein ohne vorher losgegangen zu sein!");
			}
			
			if (personId2WaitingTime.containsKey(event.getPersonId())){
				double waitingTimeSum = personId2WaitingTime.get(event.getPersonId()) + waitingTime;
				personId2WaitingTime.put(event.getPersonId(), waitingTimeSum);
			}
			
			else {
				personId2WaitingTime.put(event.getPersonId(), waitingTime);
			}
		}
		else {
			// keine Person die in einen Bus steigt!
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals("pt")){
			personId2AgentDepartureTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		
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

	/**
	 * @return the personId2WaitingTime
	 */
	public Map<Id, Double> getPersonId2WaitingTime() {
		return personId2WaitingTime;
	}

	/**
	 * @return the personId2InVehicleTime
	 */
	public Map<Id, Double> getPersonId2InVehicleTime() {
		return personId2InVehicleTime;
	}
}
