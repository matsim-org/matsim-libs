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
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;

/**
 * 
 * Holds all information needed to calculate the fare.
 * 
 * @author aneumann
 *
 */
public class FareContainer {
	
	private final double earningsPerBoardingPassenger;
	private final double earningsPerMeterAndPassenger;
	
	private PersonEntersVehicleEvent personEnterVehE;
	private TransitDriverStartsEvent transitDriverStartsE;
	private VehicleArrivesAtFacilityEvent vehArrivesAtFacilityEEntered;
	private PersonLeavesVehicleEvent personLeavesVehE;
	private VehicleArrivesAtFacilityEvent vehArrivesAtFacilityELeft;
	private double meterTravelled = 0.0;

	public FareContainer(double earningsPerBoardingPassenger, double earningsPerMeterAndPassenger){
		this.earningsPerBoardingPassenger = earningsPerBoardingPassenger;
		this.earningsPerMeterAndPassenger = earningsPerMeterAndPassenger;
	}
	
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
	
	public double getFare(){
		return this.earningsPerBoardingPassenger + this.earningsPerMeterAndPassenger * this.meterTravelled;
	}

}
