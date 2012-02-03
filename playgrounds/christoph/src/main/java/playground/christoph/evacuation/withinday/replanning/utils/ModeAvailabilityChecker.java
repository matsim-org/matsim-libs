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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.households.Household;

/**
 * Checks whether a car is available for an agent or not.
 * The position of each agent's car is logged.
 * 
 * @author cdobler
 */
public class ModeAvailabilityChecker implements AgentDepartureEventHandler, 
	PersonEntersVehicleEventHandler, ActivityStartEventHandler, SimulationInitializedListener {

	private final Scenario scenario;
	private final double maxDistance;
	private final Set<Id> drivers;
	private final Map<Id, Id> driverVehicleMapping;		// <PersonId, VehicleId>
	private final Map<Id, Coord> vehicleCoordinates;	// <VehicleId, Coordinate>
	
	/**
	 * @param scenario the simulated scenario
	 * @param maxDistance the maximum distance [m] that we allow to be between an agent and its car
	 */
	public ModeAvailabilityChecker(Scenario scenario, double maxDistance) {
		this.scenario = scenario;
		this.maxDistance = maxDistance;
		
		this.drivers = new HashSet<Id>();
		this.vehicleCoordinates = new HashMap<Id, Coord>();
		this.driverVehicleMapping = new HashMap<Id, Id>();
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
		Coord facilityCoord = facility.getCoord();
		
		List<Id> vehicles = household.getVehicleIds();
				
		List<Id> availableVehicles = new ArrayList<Id>();
		
		for (Id vehicleId : vehicles) {
			Coord carCoord = this.vehicleCoordinates.get(vehicleId);
			if (carCoord == null) continue;
			else if (CoordUtils.calcDistance(facilityCoord, carCoord) <= this.maxDistance) {
				availableVehicles.add(vehicleId);
			}
		}
		
		return availableVehicles;
	}
	
	/**
	 * @param personId Id of the person to check
	 * @param facilityId Id of the facility where the person performs an activity
	 * @return true if the person has a car available within the pre-defined maxDistance, otherwise false
	 */
	public boolean isCarAvailable(Id personId, Id facilityId) {
		
		ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
		if (facility == null) return false;
		else return this.isCarAvailable(personId, facility.getCoord());
	}
	
	/**
	 * @param personId Id of the person to check
	 * @param coord current position of the person
	 * @return true if the person has a car available within the pre-defined maxDistance, otherwise false
	 */
	public boolean isCarAvailable(Id personId, Coord coord) {
		Id vehicleId = this.driverVehicleMapping.get(personId);
		Coord carCoord = this.vehicleCoordinates.get(vehicleId);
		if (carCoord == null) return false;
		else return CoordUtils.calcDistance(coord, carCoord) <= this.maxDistance;
	}
	
	/**
	 * @param personId
	 * @return the coordinates of the car of the given person or false if the person has no car available
	 */
	public Coord getCarCoord(Id personId) {
		Id vehicleId = this.driverVehicleMapping.get(personId);
		return vehicleCoordinates.get(vehicleId);
	}
	
	/**
	 * By default we try to use a car. We can do this, if the previous or the next 
	 * Leg are performed with a car or the agents car is within a reachable distance.
	 * The order is as following:
	 * car is preferred to ride is preferred to pt is preferred to bike if preferred to walk 
	 * 
	 * @param currentActivityIndex index of an activity
	 * @param plan an agents plan plan
	 */
	public String identifyTransportMode(int currentActivityIndex, Plan plan) {

		/*
		 * check whether the agent has a car available.
		 */
		Activity currentActivity = (Activity) plan.getPlanElements().get(currentActivityIndex);
		Coord coord = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(currentActivity.getFacilityId()).getCoord();
		boolean carAvilable = this.isCarAvailable(plan.getPerson().getId(), coord);
		if (carAvilable) return TransportMode.car;
		
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
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Id vehicleId = driverVehicleMapping.get(event.getPersonId());
		if (event.getLegMode().equals(TransportMode.car)) {
			this.vehicleCoordinates.remove(vehicleId);
			this.drivers.add(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (drivers.contains(event.getPersonId())) driverVehicleMapping.put(event.getPersonId(), event.getVehicleId());		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		/*
		 * If the agent has just arrived from a car trip (and was the car's driver),
		 * we set the car's position to  the position of the agent's current facility.
		 */
		if (drivers.remove(event.getPersonId())) {	
			driverVehicleMapping.remove(event.getPersonId());
			vehicleCoordinates.put(event.getPersonId(), ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(event.getFacilityId()).getCoord());
		}
	}
	
	@Override
	public void reset(int iteration) {
		this.drivers.clear();
		this.vehicleCoordinates.clear();
		this.driverVehicleMapping.clear();
	}

	/*
	 * Get the initial coordinates of the agents cars.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		
		ActivityFacilities facilities = ((ScenarioImpl) scenario).getActivityFacilities();
		Coord activityCoord = null;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					Id facilityId = ((Activity) planElement).getFacilityId();
					activityCoord = facilities.getFacilities().get(facilityId).getCoord();
				} else if (planElement instanceof Leg) {
					/*
					 * If its a car leg, then we assume that the car is located
					 * at the coordinate of the previous activity.
					 */
					Leg leg = (Leg) planElement;
					if (leg.getMode().equals(TransportMode.car)) {
						NetworkRoute route = (NetworkRoute) leg.getRoute();
						vehicleCoordinates.put(route.getVehicleId(), activityCoord);
						break;
					}
				}
			}
		}
	}

}
