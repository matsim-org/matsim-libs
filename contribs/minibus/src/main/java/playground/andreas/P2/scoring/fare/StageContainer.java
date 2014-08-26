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

package playground.andreas.P2.scoring.fare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;

/**
 * 
 * Holds all information of one stage needed to calculate the fare.
 * 
 * @author aneumann
 *
 */
public class StageContainer {
	
	private PersonEntersVehicleEvent personEnterVehE;
	private TransitDriverStartsEvent transitDriverStartsE;
	private VehicleArrivesAtFacilityEvent vehArrivesAtFacilityEEntered;
	private PersonLeavesVehicleEvent personLeavesVehE;
	private VehicleArrivesAtFacilityEvent vehArrivesAtFacilityELeft;
	private double meterTravelled = 0.0;

	public void handlePersonEnters(PersonEntersVehicleEvent personEnterVehE, VehicleArrivesAtFacilityEvent vehArrivesAtFacilityE, TransitDriverStartsEvent transitDriverStartsE){
		this.personEnterVehE = personEnterVehE;
		this.vehArrivesAtFacilityEEntered = vehArrivesAtFacilityE;
		this.transitDriverStartsE = transitDriverStartsE;
	}
	
	public void handlePersonLeaves(PersonLeavesVehicleEvent personLeavesVehE, VehicleArrivesAtFacilityEvent vehArrivesAtFacilityE){
		this.personLeavesVehE = personLeavesVehE;
		this.vehArrivesAtFacilityELeft = vehArrivesAtFacilityE;
	}
	
	public void addDistanceTravelled(double meterTravelled){
		this.meterTravelled  += meterTravelled;
	}

	public Id getStopEntered() {
		return this.vehArrivesAtFacilityEEntered.getFacilityId();
	}

	public Id getStopLeft() {
		return this.vehArrivesAtFacilityELeft.getFacilityId();
	}
	
	public double getTimeEntered(){
		return this.personEnterVehE.getTime();
	}
	
	public double getTimeLeft(){
		return this.personLeavesVehE.getTime();
	}
	
	public Id getRouteId() {
		return this.transitDriverStartsE.getTransitRouteId();
	}
	
	public double getDistanceTravelledInMeter(){
		return this.meterTravelled;
	}
	
	public Id getVehicleId(){
		return this.transitDriverStartsE.getVehicleId();
	}
	
	public Id getAgentId(){
		return this.personLeavesVehE.getPersonId();
	}
}
