/* *********************************************************************** *
 * project: org.matsim.*
 * ModeAvailabilityChecker.java
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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;

import playground.christoph.evacuation.mobsim.VehiclesTracker;

/**
 * Checks whether a car is available for an agent or not.
 * 
 * @author cdobler
 */
public class ModeAvailabilityChecker {

	private final Scenario scenario;
	private final VehiclesTracker vehiclesTracker;
	
	/**
	 * @param scenario the simulated scenario
	 */
	public ModeAvailabilityChecker(Scenario scenario, VehiclesTracker vehiclesTracker) {
		this.scenario = scenario;
		this.vehiclesTracker = vehiclesTracker;
 	}
	
	/**
	 * Returns true, if the given person has a driving license, otherwise false.
	 * @param personId the Id of the person to check
	 * @return
	 */
	public boolean hasDrivingLicense(Id personId) {
		PersonImpl p = (PersonImpl) this.scenario.getPopulation().getPersons().get(personId);
		return p.hasLicense();
	}
	
	/**
	 * 
	 * @param householdId Id of the household to check
	 * @param facilityId Id of the facility where the household is located
	 * @return
	 */
	public List<Id> getAvailableCars(Id householdId, Id facilityId) {
		Household household = ((ScenarioImpl) scenario).getHouseholds().getHouseholds().get(householdId);
		return getAvailableCars(household, facilityId);
	}
	
	/**
	 * 
	 * @param household household to check
	 * @param facilityId Id of the facility where the household is located
	 * @return
	 */
	public List<Id> getAvailableCars(Household household, Id facilityId) {
		ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
		
		List<Id> vehicles = household.getVehicleIds();
				
		List<Id> availableVehicles = new ArrayList<Id>();
		
		for (Id vehicleId : vehicles) {
			Id carLinkId = this.vehiclesTracker.getParkedVehicles().get(vehicleId);
			if (carLinkId == null) continue;
			else if (carLinkId.equals(facility.getLinkId())) {
				availableVehicles.add(vehicleId);
			}
		}
		
		return availableVehicles;
	}
	
	/**
	 * By default we try to use a car. We can do this, if the previous or the next 
	 * Leg are performed with a car or the agents car is within a reachable distance.
	 * The order is as following:
	 * car is preferred to ride is preferred to pt is preferred to bike if preferred to walk 
	 * 
	 * @param currentActivityIndex index of an activity
	 * @param plan an agents plan plan
	 * @param possibleVehicleId id of a vehicle that the agent might use
	 */
	public String identifyTransportMode(int currentActivityIndex, Plan plan, Id possibleVehicleId) {
		/*
		 * check whether the agent has a car available.
		 */
		Activity currentActivity = (Activity) plan.getPlanElements().get(currentActivityIndex);
		boolean carAvailable = false;

		// Check whether a vehicleId was found.
		if (possibleVehicleId != null) {
			// Check whether the vehicle is currently parked.
			Id linkId = this.vehiclesTracker.getParkedVehicles().get(possibleVehicleId);
			
			if (linkId != null) {
				// Check whether the vehicle is parked at the same link where the agent performs its activity.
				if (linkId.equals(currentActivity.getLinkId())) carAvailable = true;
			}
		}
		if (carAvailable) return TransportMode.car;
		
		/*
		 * Otherwise check for the other modes 
		 */
		boolean hasBike = false;
		boolean hasPt = false;
		boolean hasRide = false;
		
		if (currentActivityIndex > 0) {
			Leg previousLeg = (Leg) plan.getPlanElements().get(currentActivityIndex - 1);
			String transportMode = previousLeg.getMode();
			if (transportMode.equals(TransportMode.bike)) hasBike = true;
			else if (transportMode.equals(TransportMode.pt)) hasPt = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (currentActivityIndex + 1 < plan.getPlanElements().size()) {
			Leg nextLeg = (Leg) plan.getPlanElements().get(currentActivityIndex + 1);
			String transportMode = nextLeg.getMode();
			if (transportMode.equals(TransportMode.bike)) hasBike = true;
			else if (transportMode.equals(TransportMode.pt)) hasPt = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (hasRide) return TransportMode.ride;
		else if (hasPt) return TransportMode.pt;
		else if (hasBike) return TransportMode.bike;
		else return TransportMode.walk;
	}

}
