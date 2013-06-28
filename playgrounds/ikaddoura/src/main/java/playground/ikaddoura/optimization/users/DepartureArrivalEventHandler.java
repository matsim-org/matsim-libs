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
package playground.ikaddoura.optimization.users;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

/**
 * @author Ihab
 *
 */
public class DepartureArrivalEventHandler implements TransitDriverStartsEventHandler, AgentDepartureEventHandler {
	private int numberOfPtLegs;
	private int numberOfCarLegs;
	private int numberOfWalkLegs; // TransitWalk
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	
	@Override
	public void reset(int iteration) {
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.numberOfPtLegs = 0;
		this.numberOfCarLegs = 0;
		this.numberOfWalkLegs = 0;
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
				
		if (!this.ptDriverIDs.contains(event.getDriverId())){
			this.ptDriverIDs.add(event.getDriverId());
		}
		
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(event.getLegMode().toString().equals(TransportMode.pt)){
			this.numberOfPtLegs++;			
		}
		
		if(!ptDriverIDs.contains(event.getPersonId()) && event.getLegMode().toString().equals(TransportMode.car)){ // legMode = car: either a bus or a car
			this.numberOfCarLegs++;
		}
				
		if(event.getLegMode().toString().equals(TransportMode.transit_walk)){
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

}
