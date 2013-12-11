/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingInfrastructure.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.core.mobsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.utils.collections.QuadTree;

import playground.christoph.parking.ParkingTypes;
import playground.christoph.parking.core.interfaces.ParkingCostCalculator;

/**
 * Class that knows the location of all parking facility and their capacities.
 * 
 * TODO: make this class thread-safe!!
 * 
 * @author cdobler
 */
public class ParkingInfrastructure {

	static final Logger log = Logger.getLogger(ParkingInfrastructure.class);

	private final QuadTree<ActivityFacility> allParkingFacilities;
	private final QuadTree<ActivityFacility> fullParkingFacilitiesWithFreeWaitingCapacity;
	private final QuadTree<ActivityFacility> availableParkingFacilities;
	private final Map<Id, List<Id>> parkingFacilitiesOnLinkMapping; // <LinkId, List<FacilityId>>
	protected final Map<Id, ParkingFacility> parkingFacilities;
	private final ParkingCostCalculator parkingCostCalculator;
	private final Set<Id> parkingFacilitiesWithWaitingAgents;
	protected final Scenario scenario;

	public void resetParkingFacilityForNewIteration() {

		for (ParkingFacility parkingFacility : this.parkingFacilities.values())
			parkingFacility.reset();

		fillQuadTree();
		
		parkingFacilitiesWithWaitingAgents.clear();
	}

	public ParkingInfrastructure(Scenario scenario, ParkingCostCalculator parkingCostCalculator) {
		this.scenario = scenario;
		this.parkingCostCalculator = parkingCostCalculator;
		this.parkingFacilities = new HashMap<Id, ParkingFacility>();

		this.parkingFacilitiesOnLinkMapping = new HashMap<Id, List<Id>>();
		this.parkingFacilitiesWithWaitingAgents = new HashSet<Id>();

//		new WorldConnectLocations(scenario.getConfig()).connectFacilitiesWithLinks(
//				scenario.getActivityFacilities(), (NetworkImpl) scenario.getNetwork());

		// Create ParkingFacilities
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {

			ActivityOption parkingOption;

			parkingOption = facility.getActivityOptions().get(ParkingTypes.PARKING);
			if (parkingOption != null) {
				int parkingCapacity = (int) Math.round(parkingOption.getCapacity());

				if (parkingCapacity > 0) {
					// so far: just assume that 10% of the parking capacity is available as waiting capacity
					int waitingCapacity = (int) (((double) parkingCapacity) / 10);
					
					ParkingFacility parkingFacility = new ParkingFacility(facility.getId(), facility.getLinkId(), 
							ParkingTypes.PARKING, parkingCapacity, waitingCapacity);
					parkingFacilities.put(facility.getId(), parkingFacility);
				}
			}
		}

		// Create a quadtree containing all parking facilities
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ParkingFacility parkingFacility : parkingFacilities.values()) {
			ActivityFacility facility = scenario.getActivityFacilities().getFacilities()
					.get(parkingFacility.getFaciltyId());
			if (facility.getCoord().getX() < minx) {
				minx = facility.getCoord().getX();
			}
			if (facility.getCoord().getY() < miny) {
				miny = facility.getCoord().getY();
			}
			if (facility.getCoord().getX() > maxx) {
				maxx = facility.getCoord().getX();
			}
			if (facility.getCoord().getY() > maxy) {
				maxy = facility.getCoord().getY();
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;

		allParkingFacilities = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		availableParkingFacilities = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		fullParkingFacilitiesWithFreeWaitingCapacity = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		
		fillQuadTree();
	}

	private void fillQuadTree() {

		this.availableParkingFacilities.clear();

		for (ParkingFacility parkingFacility : parkingFacilities.values()) {
			ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(parkingFacility.getFaciltyId());

			// add the facility to the quadtrees
			allParkingFacilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			
			if (parkingFacility.getParkingCapacity() > 0) {
				availableParkingFacilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);				
			}
			
			if (parkingFacility.getWaitingCapacity() > 0) {
				fullParkingFacilitiesWithFreeWaitingCapacity.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);				
			}

			// add the facility to the facilitiesOnLinkMapping
			Id linkId = facility.getLinkId();
			Id facilityId = facility.getId();
			assignFacilityToLink(linkId, facilityId);
		}
	}

	private void assignFacilityToLink(Id linkId, Id facilityId) {
		List<Id> list = parkingFacilitiesOnLinkMapping.get(linkId);
		if (list == null) {
			list = new ArrayList<Id>();
			parkingFacilitiesOnLinkMapping.put(linkId, list);
		}
		list.add(facilityId);
	}

	public Map<Id, ParkingFacility> getParkingFacilities() {
		return Collections.unmodifiableMap(this.parkingFacilities);
	}
	
	public int getFreeParkingCapacity(Id facilityId) {
		int freeCapacity = this.parkingFacilities.get(facilityId).getFreeParkingCapacity();

		if (freeCapacity < 0)
			throw new RuntimeException("Free parking capacity < 0 was found for facility " + facilityId.toString() + ": " + freeCapacity);

		return freeCapacity;
	}

	public boolean reserveParking(Id vehicleId, Id facilityId) {

		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);

		boolean reserved = parkingFacility.reserveParking(vehicleId);

		if (getFreeParkingCapacity(facilityId) <= 0) {
			markFacilityAsFull(facilityId);
		}

		if (!reserved) {
			throw new RuntimeException("Could not reserve parking for vehicle " + vehicleId.toString() + 
					" at parking facility " + facilityId.toString());
		}
		return reserved;
	}

	public boolean unReserveParking(Id vehicleId, Id facilityId) {

		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);
		return parkingFacility.unReserveParking(vehicleId);
	}

	public int getFreeWaitingCapacity(Id facilityId) {
		int freeCapacity = this.parkingFacilities.get(facilityId).getFreeWaitingCapacity();

		if (freeCapacity < 0)
			throw new RuntimeException("Free waiting capacity < 0 was found for facility " + facilityId.toString() + ": " + freeCapacity);

		return freeCapacity;
	}
	
	public boolean reserveWaiting(Id vehicleId, Id facilityId) {

		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);

		boolean reserved = parkingFacility.reserveWaiting(vehicleId);

//		if (getFreeWaitingCapacity(facilityId) == 0) {
//			markFacilityAsFull(facilityId);
//		}

		if (!reserved) {
			throw new RuntimeException("Could not reserve parking for vehicle " + vehicleId.toString() + 
					" at parking facility " + facilityId.toString());
		}
		return reserved;
	}

	public boolean unReserveWaiting(Id vehicleId, Id facilityId) {

		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);
		return parkingFacility.unReserveWaiting(vehicleId);
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @param facilityId
	 * @returns <b>true</b> if the vehicle was parked or<br>
	 * 		<b>false</b> if the agent has to wait until a parking spot becomes available.
	 */
	public boolean parkVehicle(Id vehicleId, Id facilityId) {
		
		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);
		boolean waiting = false;
		
		// try to park the vehicle in the parking facility
		boolean parked = parkingFacility.reservedToOccupied(vehicleId);
		if (parked) {
			return true;
		} 
		// else: check whether the agent was registered as waiting for free parking
		else {
			waiting = parkingFacility.reservedToWaiting(vehicleId);
			if (waiting) {
				// maybe now a free parking is available
				if (parkingFacility.getFreeParkingCapacity() > 0) {
					parkingFacility.unReserveWaiting(vehicleId);
					parkingFacility.reserveParking(vehicleId);
					parkingFacility.reservedToOccupied(vehicleId);
					return true;
				} else {
					this.parkingFacilitiesWithWaitingAgents.add(facilityId);
					return false;
				}
			} 	
		}		

		if (!parked && !waiting) {
			throw new RuntimeException("Could not park vehicle " + vehicleId.toString() + 
					" at parking facility " + facilityId.toString());			
		}
		
		return false;
	}
	
	public void unParkVehicle(Id vehicleId, Id facilityId) {

		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);
		boolean unparked = parkingFacility.release(vehicleId);

		if (!unparked) {
			throw new RuntimeException("Could not unpark vehicle " + vehicleId.toString() + 
					" at parking facility  " + facilityId.toString());
		}

		if (getFreeParkingCapacity(facilityId) == 1) {
			markFacilityAsNonFull(facilityId);
		}
	}

	// returns a collection containing all agent Ids which have been moved from waiting to parking
	public Collection<Id> waitingToParking() {
		
		List<Id> parkedAgentIds = new ArrayList<Id>();
		
		Iterator<Id> iter = this.parkingFacilitiesWithWaitingAgents.iterator();
		while (iter.hasNext()) {
			Id parkingFacilityId = iter.next();
			
			ParkingFacility parkingFacility = this.parkingFacilities.get(parkingFacilityId);

			int freeParkingCapacity = parkingFacility.getFreeParkingCapacity();
			for (int i = 0; i < freeParkingCapacity; i++) {
				Id parkedAgentId = parkingFacility.nextWaitingToOccupied(); 
				if (parkedAgentId == null) break;
				else parkedAgentIds.add(parkedAgentId);				
			}
			
			// remove facility from list if no more agents are waiting
			if (parkingFacility.getWaitingCapacity() == parkingFacility.getFreeWaitingCapacity()) {
				iter.remove();
			}
		}
		return parkedAgentIds;
	}
	
	private void markFacilityAsFull(Id facilityId) {
		ActivityFacility activityFacility = scenario.getActivityFacilities().getFacilities().get(facilityId);
		this.availableParkingFacilities.remove(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(),
				activityFacility);
	}

	private void markFacilityAsNonFull(Id facilityId) {
		ActivityFacility activityFacility = scenario.getActivityFacilities().getFacilities().get(facilityId);
		this.availableParkingFacilities.put(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(),
				activityFacility);
	}

	public List<Id> getParkingsOnLink(Id linkId) {
		return parkingFacilitiesOnLinkMapping.get(linkId);
	}

	public List<Id> getFreeParkingFacilitiesOnLink(Id linkId, String parkingType) {

		List<Id> parkings = new ArrayList<Id>();

		List<Id> list = getParkingsOnLink(linkId);

		// if no parkings are available on the link, return an empty list
		if (list == null) return parkings;

		for (Id parkingId : list) {
			ParkingFacility parkingFacility = this.parkingFacilities.get(parkingId);

			// check parking type
			if (parkingFacility.getParkingType().equals(parkingType)) {
				
				// check free capacity
				if (parkingFacility.getFreeParkingCapacity() > 0) parkings.add(parkingId);
			}
		}

		return parkings;
	}

	public List<Id> getFreeWaitingFacilitiesOnLink(Id linkId, String parkingType) {

		List<Id> parkings = new ArrayList<Id>();

		List<Id> list = getParkingsOnLink(linkId);

		// if no parkings are available on the link, return an empty list
		if (list == null) return parkings;

		for (Id parkingId : list) {
			ParkingFacility parkingFacility = this.parkingFacilities.get(parkingId);

			// check parking type
			if (parkingFacility.getParkingType().equals(parkingType)) {
				
				// check free capacity
				if (parkingFacility.getFreeWaitingCapacity() > 0) parkings.add(parkingId);
			}
		}

		return parkings;
	}
	
	public ActivityFacility getClosestFreeParkingFacility(Coord coord) {
		return this.availableParkingFacilities.get(coord.getX(), coord.getY());
	}

	public ActivityFacility getClosestParkingFacility(Coord coord) {
		return this.allParkingFacilities.get(coord.getX(), coord.getY());
	}

	public ParkingCostCalculator getParkingCostCalculator() {
		return parkingCostCalculator;
	}

	public Collection<ActivityFacility> getAllFreeParkingWithinDistance(double distance, Coord coord) {

		Collection<ActivityFacility> parkings = this.availableParkingFacilities.get(coord.getX(), coord.getY(), distance);

		return parkings;
	}

	/*
	 * So far, use this simple approach. Later use a lookup map or something
	 * similar.
	 */
	public Id getVehicleId(Person person) {
		return person.getId();
	}

	public void printStatistics() {
		
		double parkingCapacity = 0.0;
		double waitingCapacity = 0.0;
		double freeParkingCapacity = 0.0;
		double freeWaitingCapacity = 0.0;
		double occupiedParkingCapacity = 0.0;
		double occupiedWaitingCapacity = 0.0;
		for (ParkingFacility parkingFacility : this.parkingFacilities.values()) {
			parkingCapacity += parkingFacility.getParkingCapacity();
			waitingCapacity += parkingFacility.getWaitingCapacity();

			freeParkingCapacity += parkingFacility.getFreeParkingCapacity();
			freeWaitingCapacity += parkingFacility.getFreeWaitingCapacity();
			
			occupiedParkingCapacity += (parkingFacility.getParkingCapacity() - parkingFacility.getFreeParkingCapacity());
			occupiedWaitingCapacity += (parkingFacility.getWaitingCapacity() - parkingFacility.getFreeWaitingCapacity());
		}
		
		log.info("Number of parking facilities: " + this.parkingFacilities.size());
		log.info("Parking capacity: " + parkingCapacity);
		log.info("Waiting capacity: " + waitingCapacity);
		log.info("Free parking capacity: " + freeParkingCapacity);
		log.info("Free waiting capacity: " + freeWaitingCapacity);
		log.info("Occupied parking capacity: " + occupiedParkingCapacity);
		log.info("Occupied waiting capacity: " + occupiedWaitingCapacity);
	}

}
