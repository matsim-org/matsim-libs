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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import org.matsim.withinday.mobsim.MobsimDataProvider;


/**
 * Checks whether a car is available for an agent (respectively a household) or not.
 * 
 * @author cdobler
 */
public class ModeAvailabilityChecker {

	private final Scenario scenario;
	private final Vehicles vehicles;
	private final MobsimDataProvider mobsimDataProvider;
	private final WalkSpeedComparator walkSpeedComparator;
	
	public ModeAvailabilityChecker(Scenario scenario, MobsimDataProvider mobsimDataProvider) {
		this.scenario = scenario;
		Logger.getLogger(this.getClass()).fatal("cannot say if the following should be vehicles or transit vehicles; aborting ... .  kai, feb'15");
		System.exit(-1); 
		this.vehicles = ((ScenarioImpl) scenario).getTransitVehicles();
		this.mobsimDataProvider = mobsimDataProvider;
		this.walkSpeedComparator = new WalkSpeedComparator();
		
		// initialize walkSpeedComparator
		this.walkSpeedComparator.calcTravelTimes(scenario.getPopulation());
 	}

	// only used when creating a new instance
	private ModeAvailabilityChecker(Scenario scenario, MobsimDataProvider mobsimDataProvider, 
			WalkSpeedComparator walkSpeedComparator) {
		this.scenario = scenario;
		Logger.getLogger(this.getClass()).fatal("cannot say if the following should be vehicles or transit vehicles; aborting ... .  kai, feb'15");
		System.exit(-1); 
		this.vehicles = ((ScenarioImpl) scenario).getTransitVehicles();
		this.mobsimDataProvider = mobsimDataProvider;
		this.walkSpeedComparator = walkSpeedComparator;		
	}
	
	public ModeAvailabilityChecker createInstance() {
		return new ModeAvailabilityChecker(scenario, mobsimDataProvider, walkSpeedComparator);
	}
	
	/**
	 * Returns true, if the given person has a driving license, otherwise false.
	 * @param personId the Id of the person to check
	 * @return
	 */
	public boolean hasDrivingLicense(Id personId) {
		Person p = this.scenario.getPopulation().getPersons().get(personId);
		return PersonImpl.hasLicense(p);
	}
	
	/**
	 * 
	 * @param householdId Id of the household to check
	 * @param facilityId Id of the facility where the household is located
	 * @return
	 */
	public List<Id<Vehicle>> getAvailableCars(Id<Household> householdId, Id<ActivityFacility> facilityId) {
		Household household = ((ScenarioImpl) scenario).getHouseholds().getHouseholds().get(householdId);
		return getAvailableCars(household, facilityId);
	}
	
	/**
	 * 
	 * @param household household to check
	 * @param facilityId Id of the facility where the household is located
	 * @return
	 */
	public List<Id<Vehicle>> getAvailableCars(Household household, Id<ActivityFacility> facilityId) {
		ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(facilityId);
		
		List<Id<Vehicle>> vehicles = household.getVehicleIds();
				
		List<Id<Vehicle>> availableVehicles = new ArrayList<Id<Vehicle>>();
		
		for (Id<Vehicle> vehicleId : vehicles) {
			MobsimVehicle vehicle = this.mobsimDataProvider.getVehicle(vehicleId);
			
			// if the driver is null, the vehicle is parked 
			boolean isParked = (vehicle.getDriver() == null);
			if (!isParked) continue;
			
			Id carLinkId = vehicle.getCurrentLink().getId();
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
			
			MobsimVehicle vehicle = this.mobsimDataProvider.getVehicle(possibleVehicleId);
			
			// if the driver is null, the vehicle is parked 
			boolean isParked = (vehicle.getDriver() == null);
			if (isParked) {
				Id linkId = vehicle.getCurrentLink().getId();
				
				// Check whether the vehicle is parked at the same link where the agent performs its activity.
				if (linkId.equals(currentActivity.getLinkId())) carAvailable = true;
			}
		}
		if (carAvailable) return TransportMode.car;
		
		/*
		 * Otherwise check for the other modes 
		 */
		boolean hasBike = false;
//		boolean hasPt = false;
		boolean hasRide = false;
		
		if (currentActivityIndex > 0) {
			Leg previousLeg = (Leg) plan.getPlanElements().get(currentActivityIndex - 1);
			String transportMode = previousLeg.getMode();
			if (transportMode.equals(TransportMode.bike)) hasBike = true;
//			else if (transportMode.equals(TransportMode.pt)) hasPt = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (currentActivityIndex + 1 < plan.getPlanElements().size()) {
			Leg nextLeg = (Leg) plan.getPlanElements().get(currentActivityIndex + 1);
			String transportMode = nextLeg.getMode();
			if (transportMode.equals(TransportMode.bike)) hasBike = true;
//			else if (transportMode.equals(TransportMode.pt)) hasPt = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (hasRide) return TransportMode.ride;
//		else if (hasPt) return TransportMode.pt;
		else if (hasBike) return TransportMode.bike;
		else return TransportMode.walk;
	}
			
	private Queue<Vehicle> getVehiclesQueue(List<Id<Vehicle>> vehicleIds) {
		Queue<Vehicle> queue = new PriorityQueue<Vehicle>(2, new VehicleSeatsComparator());
		for (Id<Vehicle> id : vehicleIds) {
			Vehicle vehicle = vehicles.getVehicles().get(id);
			queue.add(vehicle);
		}
		return queue;
	}
	
	/*
	 * For decisions on household level. 
	 */
	public HouseholdModeAssignment getHouseholdModeAssignment(Household household, Id<ActivityFacility> facilityId) {
		List<Id<Vehicle>> availableVehicleIds = this.getAvailableCars(household, facilityId);
		Queue<Vehicle> availableVehicles = getVehiclesQueue(availableVehicleIds);
		return getHouseholdModeAssignment(household.getMemberIds(), availableVehicles, facilityId);
	}
	
	/*
	 * For decisions on non-household or only part-household level.
	 */
	public HouseholdModeAssignment getHouseholdModeAssignment(Collection<Id<Person>> personIds, 
			Collection<Id<Vehicle>> vehicleIds, Id<ActivityFacility> facilityId) {
		
		Queue<Vehicle> queue = new PriorityQueue<Vehicle>(2, new VehicleSeatsComparator());
		for (Id<Vehicle> id : vehicleIds) {
			Vehicle vehicle = vehicles.getVehicles().get(id);
			queue.add(vehicle);
		}
		return this.getHouseholdModeAssignment(personIds, queue, facilityId);
	}
	
	private HouseholdModeAssignment getHouseholdModeAssignment(Collection<Id<Person>> personIds, Queue<Vehicle> availableVehicles, Id facilityId) {	
		HouseholdModeAssignment assignment = new HouseholdModeAssignment();
		
		Queue<Id> possibleDrivers = new PriorityQueue<Id>(4, walkSpeedComparator);
		Queue<Id> possiblePassengers = new PriorityQueue<Id>(4, walkSpeedComparator);
		
		// identify potential drivers and passengers
		for (Id personId : personIds) {
			if (this.hasDrivingLicense(personId)) possibleDrivers.add(personId);
			else possiblePassengers.add(personId);					
		}
		
		/*
		 * Fill people into vehicles. Start with largest cars.
		 * Will end if all people are assigned to a vehicle
		 * or no further vehicles or drivers a available.
		 * Remaining agents will walk.
		 */
		while (availableVehicles.peek() != null) {
			Vehicle vehicle = availableVehicles.poll();
			int seats = vehicle.getType().getCapacity().getSeats();
			
			// if no more drivers are available
			if (possibleDrivers.peek() == null) break;
			
			// set transport mode for driver
			Id driverId = possibleDrivers.poll();
			assignment.addTransportModeMapping(driverId, TransportMode.car);
			assignment.addDriverVehicleMapping(driverId, vehicle.getId());
			seats--;
			
			List<Id> passengers = new ArrayList<Id>();
			while (seats > 0) {
				Id passengerId = null;
				if (possiblePassengers.peek() != null) {
					passengerId = possiblePassengers.poll();
				} else if (possibleDrivers.peek() != null) {
					passengerId = possibleDrivers.poll();
				} else {
					break;
				}
				
				passengers.add(passengerId);
				assignment.addTransportModeMapping(passengerId, PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE);
				seats--;
			}
			
			// register person as passenger in the vehicle
			for (Id passengerId : passengers) {
				assignment.addPassengerVehicleMapping(passengerId, vehicle.getId());
			}
			
		}
		// if vehicle capacity is exceeded, remaining agents have to walk
		while (possibleDrivers.peek() != null) {
			assignment.addTransportModeMapping(possibleDrivers.poll(), TransportMode.walk);
		}
		while (possiblePassengers.peek() != null) {
			assignment.addTransportModeMapping(possiblePassengers.poll(), TransportMode.walk);
		}
		
		return assignment;
	}
	
	private static class VehicleSeatsComparator implements Comparator<Vehicle>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;
		
		@Override
		public int compare(Vehicle v1, Vehicle v2) {
			
			int seats1 = v1.getType().getCapacity().getSeats();
			int seats2 = v2.getType().getCapacity().getSeats();
			
			if (seats1 > seats2) return 1;
			else if (seats1 < seats2) return -1;
			// both have the same number of seats: order them based on their Id
			else return v1.getId().compareTo(v2.getId());
		}
	}
}
