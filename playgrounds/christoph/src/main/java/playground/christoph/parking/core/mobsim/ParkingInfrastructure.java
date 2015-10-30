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
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.vehicles.Vehicle;

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
//	private final QuadTree<ActivityFacility> fullParkingFacilitiesWithFreeWaitingCapacity;
//	private final QuadTree<ActivityFacility> availableParkingFacilities;
	private final Map<String, QuadTree<ActivityFacility>> availableParkingFacilities;
	private final Map<Id<Link>, Map<String, List<Id<ParkingFacility>>>> parkingFacilitiesOnLinkMapping; // <LinkId, Map<ParkingType, List<FacilityId>>>
	protected final Map<Id, ParkingFacility> parkingFacilities;
	private final ParkingCostCalculator parkingCostCalculator;
	private final Set<Id> parkingFacilitiesWithWaitingAgents;
	private final Set<String> parkingTypes;
	protected final Scenario scenario;

	public ParkingInfrastructure(Scenario scenario, ParkingCostCalculator parkingCostCalculator, Set<String> parkingTypes, double capacityFactor) {
		this.scenario = scenario;
		this.parkingCostCalculator = parkingCostCalculator;
		this.parkingFacilities = new HashMap<Id, ParkingFacility>();
		this.parkingTypes = new TreeSet<String>(parkingTypes);
		
		this.parkingFacilitiesWithWaitingAgents = new HashSet<Id>();
		this.parkingFacilitiesOnLinkMapping = new HashMap<Id<Link>, Map<String, List<Id<ParkingFacility>>>>();
				
		// create ParkingFacility objects and add facilities to the facilitiesOnLinkMapping
		for (String parkingType : this.parkingTypes) {
			Map<Id<ActivityFacility>, ActivityFacility> parkingFacilitiesForType = this.scenario.getActivityFacilities().getFacilitiesForActivityType(parkingType);
			
			for (ActivityFacility facility : parkingFacilitiesForType.values()) {
				Id facilityId = facility.getId();
				Id linkId = facility.getLinkId();
				int parkingCapacity = (int) Math.round(facility.getActivityOptions().get(parkingType).getCapacity() * capacityFactor);
				int waitingCapacity = 0;
				ActivityOption waitingOption = facility.getActivityOptions().get(ParkingFacility.WAITINGCAPACITY);
				if (waitingOption != null) waitingCapacity = (int) Math.round(waitingOption.getCapacity() * capacityFactor);
				
				ParkingFacility parkingFacility = new ParkingFacility(facilityId, linkId, parkingType, parkingCapacity, waitingCapacity);
				this.parkingFacilities.put(facilityId, parkingFacility);
				
				assignFacilityToLink(linkId, facilityId, parkingFacility.getParkingType());

			}
		}
		
//		/*
//		 * Create waiting capacity for parking activities 
//		 */
//		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
//
//			ActivityOption parkingOption;
//
//			parkingOption = facility.getActivityOptions().get(ParkingTypes.PARKING);
//			if (parkingOption != null) {
//				int parkingCapacity = (int) Math.round(parkingOption.getCapacity());
//
//				if (parkingCapacity > 0) {
//					// so far: just assume that 10% of the parking capacity is available as waiting capacity
//					int waitingCapacity = (int) (((double) parkingCapacity) / 10);
//					
//					ParkingFacility parkingFacility = new ParkingFacility(facility.getId(), facility.getLinkId(), 
//							ParkingTypes.PARKING, parkingCapacity, waitingCapacity);
//					parkingFacilities.put(facility.getId(), parkingFacility);
//				}
//			}
//		}

		// Create a quadtree containing all parking facilities
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ParkingFacility parkingFacility : this.parkingFacilities.values()) {
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

		this.allParkingFacilities = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);

//		this.availableParkingFacilities = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		this.availableParkingFacilities = new HashMap<String, QuadTree<ActivityFacility>>();
		for (String parkingType : this.parkingTypes) this.availableParkingFacilities.put(parkingType, new QuadTree<ActivityFacility>(minx, miny, maxx, maxy));
		
//		this.fullParkingFacilitiesWithFreeWaitingCapacity = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		
		fillQuadTree(this.parkingTypes);
	}

	public void resetParkingFacilityForNewIteration() {

		this.allParkingFacilities.clear();
		for (ParkingFacility parkingFacility : this.parkingFacilities.values()) parkingFacility.reset();
		
		fillQuadTree(this.parkingTypes);
		
		this.parkingFacilitiesWithWaitingAgents.clear();
	}

	
	private void fillQuadTree(Set<String> parkingTypes) {

		for (String parkingType : parkingTypes) this.availableParkingFacilities.get(parkingType).clear();

		for (ParkingFacility parkingFacility : parkingFacilities.values()) {
			ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(parkingFacility.getFaciltyId());

			// add the facility to the quadtrees
			this.allParkingFacilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			
			if (parkingFacility.getParkingCapacity() > 0) {
				this.availableParkingFacilities.get(parkingFacility.getParkingType()).put(facility.getCoord().getX(), 
						facility.getCoord().getY(), facility);				
			}
			
//			if (parkingFacility.getWaitingCapacity() > 0) {
//				fullParkingFacilitiesWithFreeWaitingCapacity.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);				
//			}
		}
	}

	private void assignFacilityToLink(Id<Link> linkId, Id<ParkingFacility> facilityId, String parkingType) {
		
		Map<String, List<Id<ParkingFacility>>> map = this.parkingFacilitiesOnLinkMapping.get(linkId);
		if (map == null) {
			map = new HashMap<String, List<Id<ParkingFacility>>>();
			this.parkingFacilitiesOnLinkMapping.put(linkId, map);
		}
		
		List<Id<ParkingFacility>> list = map.get(parkingType);
		if (list == null) {
			list = new ArrayList<Id<ParkingFacility>>();
			map.put(parkingType, list);
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
			markFacilityAsFull(facilityId, parkingFacility.getParkingType());
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
		
		if (parkingFacility == null) {
			System.out.println("null");
		}
		
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
			markFacilityAsNonFull(facilityId, parkingFacility.getParkingType());
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
	
	private void markFacilityAsFull(Id facilityId, String parkingType) {
		ActivityFacility activityFacility = scenario.getActivityFacilities().getFacilities().get(facilityId);
		this.availableParkingFacilities.get(parkingType).remove(activityFacility.getCoord().getX(), 
				activityFacility.getCoord().getY(), activityFacility);
	}

	private void markFacilityAsNonFull(Id facilityId, String parkingType) {
		ActivityFacility activityFacility = scenario.getActivityFacilities().getFacilities().get(facilityId);
		this.availableParkingFacilities.get(parkingType).put(activityFacility.getCoord().getX(), 
				activityFacility.getCoord().getY(), activityFacility);
	}

	public List<Id<ParkingFacility>> getParkingsOnLink(Id<Link> linkId, String parkingType) {
		
		Map<String, List<Id<ParkingFacility>>> map = this.parkingFacilitiesOnLinkMapping.get(linkId);
		if (map == null) return null;
		else return map.get(parkingType);
	}

	public List<Id<ParkingFacility>> getFreeParkingFacilitiesOnLink(Id<Link> linkId, String parkingType) {

		List<Id<ParkingFacility>> parkings = new ArrayList<Id<ParkingFacility>>();

		List<Id<ParkingFacility>> list = getParkingsOnLink(linkId, parkingType);

		// if no parkings are available on the link, return an empty list
		if (list == null) return parkings;

		for (Id<ParkingFacility> parkingId : list) {
			ParkingFacility parkingFacility = this.parkingFacilities.get(parkingId);

			// check parking type
			if (parkingFacility.getParkingType().equals(parkingType)) {
				
				// check free capacity
				if (parkingFacility.getFreeParkingCapacity() > 0) parkings.add(parkingId);
			}
		}

		return parkings;
	}

	public List<Id<ParkingFacility>> getFreeWaitingFacilitiesOnLink(Id<Link> linkId, String parkingType) {

		List<Id<ParkingFacility>> parkings = new ArrayList<Id<ParkingFacility>>();

		List<Id<ParkingFacility>> list = getParkingsOnLink(linkId, parkingType);

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
	
	public ActivityFacility getClosestFreeParkingFacility(Coord coord, String parkingType) {
		return this.availableParkingFacilities.get(parkingType).getClosest(coord.getX(), coord.getY());
	}

	public ActivityFacility getClosestParkingFacility(Coord coord) {
		return this.allParkingFacilities.getClosest(coord.getX(), coord.getY());
	}

	public ParkingCostCalculator getParkingCostCalculator() {
		return parkingCostCalculator;
	}

	public Collection<ActivityFacility> getAllFreeParkingWithinDistance(double distance, Coord coord, String parkingType) {

		Collection<ActivityFacility> parkings = this.availableParkingFacilities.get(parkingType).getDisk(coord.getX(), coord.getY(), distance);

		return parkings;
	}

	/*
	 * So far, use this simple approach. Later use a lookup map or something
	 * similar.
	 */
	public Id<Vehicle> getVehicleId(Person person) {
		return Id.create(person.getId(), Vehicle.class);
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
