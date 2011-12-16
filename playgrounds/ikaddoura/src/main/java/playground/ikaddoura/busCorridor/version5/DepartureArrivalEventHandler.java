/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureEventHandler.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.version5;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

/**
 * @author Ihab
 *
 */
public class DepartureArrivalEventHandler implements AgentDepartureEventHandler, AgentArrivalEventHandler{
	private int numberOfPtLegs;
	private int numberOfCarLegs;
	private int numberOfWalkLegs; // Walk & TransitWalk
	private double vehicleSeconds;
	
	private Map<Id, Double> personID2firstDepartureTime = new HashMap<Id, Double>();
	private Map<Id, Double> personID2lastArrivalTime = new HashMap<Id, Double>();
	
	@Override
	public void reset(int iteration) {
		this.vehicleSeconds = 0;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(event.getLegMode().toString().equals("pt")){
			this.numberOfPtLegs++;			
		}
		if(event.getLegMode().toString().equals("car")){ // either a bus or a car
			if (!event.getPersonId().toString().contains("bus")){
				this.numberOfCarLegs++;
			}
			if (event.getPersonId().toString().contains("bus")){
				Id id = event.getPersonId();
				if (id.toString().contains("bus")){
					if (this.personID2firstDepartureTime.containsKey(event.getPersonId())){
						if (event.getTime() < this.personID2firstDepartureTime.get(event.getPersonId())){
							this.personID2firstDepartureTime.put(id, event.getTime());
						}
						else {}
					}
					else {
						this.personID2firstDepartureTime.put(id, event.getTime());
					}
				}
			}
			else {}
		}
		if(event.getLegMode().toString().equals("walk") || event.getLegMode().toString().equals("transit_walk")){
			this.numberOfWalkLegs++;
		}
	}

	public int getNumberOfPtLegs() {
		return this.numberOfPtLegs;
	}

	public int getNumberOfCarLegs() {
		return this.numberOfCarLegs;
	}
	
	public int getNumberOfWalkLegs() {
		return this.numberOfWalkLegs;
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		
		if(event.getLegMode().toString().equals("car")){ // either a bus or a car
			if (!event.getPersonId().toString().contains("bus")){
				this.numberOfCarLegs++;
			}
			if (event.getPersonId().toString().contains("bus")){
				Id id = event.getPersonId();
				if (id.toString().contains("bus")){
					if (this.personID2lastArrivalTime.containsKey(event.getPersonId())){
						if (event.getTime() > this.personID2lastArrivalTime.get(event.getPersonId())){
							this.personID2lastArrivalTime.put(id, event.getTime());
						}
						else {}
					}
					else {
						this.personID2lastArrivalTime.put(id, event.getTime());
					}
				}
			}
			else {}
		}
		
	}

	public double getVehicleHours() {
		for (Id id : this.personID2firstDepartureTime.keySet()){
//			System.out.println(id);
//			System.out.println(" --> first departure time: "+this.personID2firstDepartureTime);
//			System.out.println(" --> last arrival time: "+this.personID2lastArrivalTime);
//			System.out.println("----------");

			this.vehicleSeconds = this.vehicleSeconds + ((this.personID2lastArrivalTime.get(id) - this.personID2firstDepartureTime.get(id)));
		}
		return vehicleSeconds / 3600;
	}

}
