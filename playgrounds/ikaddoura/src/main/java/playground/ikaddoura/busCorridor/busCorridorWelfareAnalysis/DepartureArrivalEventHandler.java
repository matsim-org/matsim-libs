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
package playground.ikaddoura.busCorridor.busCorridorWelfareAnalysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

/**
 * @author Ihab
 *
 */
public class DepartureArrivalEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler{
	private int numberOfPtLegs;
	private int numberOfCarLegs;
	private int numberOfWalkLegs; // Walk, not Transit Walk!
	private double vehicleSeconds;
	
	private Map<Id, Double> personID2firstDepartureTime = new HashMap<Id, Double>();
	private Map<Id, Double> personID2lastArrivalTime = new HashMap<Id, Double>();
	
	@Override
	public void reset(int iteration) {
		this.vehicleSeconds = 0;
		this.numberOfPtLegs = 0;
		this.numberOfCarLegs = 0;
		this.numberOfWalkLegs = 0;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getLegMode().toString().equals("pt")){
			this.numberOfPtLegs++;			
		}
		
		if(event.getLegMode().toString().equals("car") && !event.getPersonId().toString().contains("bus")){ // legMode = car: either a bus or a car
			this.numberOfCarLegs++;
		}
		
		if(event.getLegMode().toString().equals("car") && event.getPersonId().toString().contains("bus")){
			if (this.personID2firstDepartureTime.containsKey(event.getPersonId())){
				if (event.getTime() < this.personID2firstDepartureTime.get(event.getPersonId())){
					this.personID2firstDepartureTime.put(event.getPersonId(), event.getTime());
				}
				else {}
			}
			else {
				this.personID2firstDepartureTime.put(event.getPersonId(), event.getTime());
			}
		}
		
		if(event.getLegMode().toString().equals("walk")){
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
	public void handleEvent(PersonArrivalEvent event) {
		
		if(event.getLegMode().toString().equals("car") && event.getPersonId().toString().contains("bus")){ // either a bus or a car
			if (this.personID2lastArrivalTime.containsKey(event.getPersonId())){
				if (event.getTime() > this.personID2lastArrivalTime.get(event.getPersonId())){
					this.personID2lastArrivalTime.put(event.getPersonId(), event.getTime());
				}
				else {}
			}
			else {
				this.personID2lastArrivalTime.put(event.getPersonId(), event.getTime());
			}
		}
	}
	

	public double getVehicleHours() {
		for (Id id : this.personID2firstDepartureTime.keySet()){
			this.vehicleSeconds = this.vehicleSeconds + ((this.personID2lastArrivalTime.get(id) - this.personID2firstDepartureTime.get(id)));
		}
		return vehicleSeconds / 3600;
	}

}
