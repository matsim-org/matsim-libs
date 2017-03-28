/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus.fare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * Holds all information of one stage needed to calculate the fare.
 * 
 * @author aneumann
 *
 */
public final class StageContainer {
	
	private PersonEntersVehicleEvent personEnterVehE;
	private TransitDriverStartsEvent transitDriverStartsE;
	private VehicleArrivesAtFacilityEvent vehArrivesAtFacilityEEntered;
	private PersonLeavesVehicleEvent personLeavesVehE;
	private VehicleArrivesAtFacilityEvent vehArrivesAtFacilityELeft;
	private double meterTravelled = 0.0;

	public void handlePersonEnters(PersonEntersVehicleEvent personEnterVehE1, VehicleArrivesAtFacilityEvent vehArrivesAtFacilityE, TransitDriverStartsEvent transitDriverStartsE1){
		this.personEnterVehE = personEnterVehE1;
		this.vehArrivesAtFacilityEEntered = vehArrivesAtFacilityE;
		this.transitDriverStartsE = transitDriverStartsE1;
	}
	
	public void handlePersonLeaves(PersonLeavesVehicleEvent personLeavesVehE1, VehicleArrivesAtFacilityEvent vehArrivesAtFacilityE){
		this.personLeavesVehE = personLeavesVehE1;
		this.vehArrivesAtFacilityELeft = vehArrivesAtFacilityE;
	}
	
	public void addDistanceTravelled(double meterTravelled1){
		this.meterTravelled  += meterTravelled1;
	}

	public Id<TransitStopFacility> getStopEntered() {
		return this.vehArrivesAtFacilityEEntered.getFacilityId();
	}

	public Id<TransitStopFacility> getStopLeft() {
		return this.vehArrivesAtFacilityELeft.getFacilityId();
	}
	
	public double getTimeEntered(){
		return this.personEnterVehE.getTime();
	}
	
	public double getTimeLeft(){
		return this.personLeavesVehE.getTime();
	}
	
	public Id<TransitRoute> getRouteId() {
		return this.transitDriverStartsE.getTransitRouteId();
	}
	
	public double getDistanceTravelledInMeter(){
		return this.meterTravelled;
	}
	
	public Id<Vehicle> getVehicleId(){
		return this.transitDriverStartsE.getVehicleId();
	}
	
	public Id<Person> getAgentId(){
		return this.personLeavesVehE.getPersonId();
	}
	
	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append("StartStop " + getStopEntered());
		strB.append("; EndStop " + getStopLeft());
		strB.append("; TimeEntered " + getTimeEntered());
		strB.append("; getTimeLeft " + getTimeLeft());
		strB.append("; RouteId " + getRouteId());
		strB.append("; distanceMeter " + getDistanceTravelledInMeter());
		strB.append("; vehId " + getVehicleId());
		strB.append("; agentId " + getAgentId());
		return strB.toString();
	}
}
