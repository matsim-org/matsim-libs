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
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import playground.christoph.parking.core.interfaces.ParkingCostCalculator;

public class ParkingInfrastructure  {

	private static final Logger log = Logger.getLogger(ParkingInfrastructure.class);
	
	private final QuadTree<ActivityFacility> allParkingFacilities;
	private final QuadTree<ActivityFacility> availableParkingFacilities;
	private final Map<Id, List<Id>> parkingFacilitiesOnLinkMapping; // <LinkId, List<FacilityId>>
	protected final Map<Id, ParkingFacility> parkingFacilities;
	private final ParkingCostCalculator parkingCostCalculator;
	private final Scenario scenario;	
	
	public void resetParkingFacilityForNewIteration() {
		
		for (ParkingFacility parkingFacility : parkingFacilities.values()) parkingFacility.reset();
		
		fillQuadTree();
	}

	public ParkingInfrastructure(Scenario scenario, ParkingCostCalculator parkingCostCalculator) {
		this.scenario = scenario;
		this.parkingCostCalculator = parkingCostCalculator;
		parkingFacilities = new HashMap<Id, ParkingFacility>();

		parkingFacilitiesOnLinkMapping = new HashMap<Id, List<Id>>();
		
		new WorldConnectLocations(scenario.getConfig()).connectFacilitiesWithLinks(((ScenarioImpl) scenario).getActivityFacilities(), (NetworkImpl) scenario.getNetwork());
		
		// Create ParkingFacilities
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			
			ActivityOption parkingOption;

			parkingOption = facility.getActivityOptions().get("parking");
			if (parkingOption != null) {
				int capacity = (int) Math.round(parkingOption.getCapacity());
				
				if (capacity > 0) {
					ParkingFacility parkingFacility = new ParkingFacility(facility.getId(), facility.getLinkId(), "streetParking", capacity);
					parkingFacilities.put(facility.getId(), parkingFacility);
				}
			}
			
//			parkingOption = facility.getActivityOptions().get("streetParking");
//			if (parkingOption != null) {
//				int capacity = (int) Math.round(parkingOption.getCapacity());
//				
//				if (capacity > 0) {
//					ParkingFacility parkingFacility = new ParkingFacility(facility.getId(), facility.getLinkId(), "streetParking", capacity);
//					parkingFacilities.put(facility.getId(), parkingFacility);
//				}
//			}
//			
//			parkingOption = facility.getActivityOptions().get("garageParking");
//			if (parkingOption != null) {
//				int capacity = (int) Math.round(parkingOption.getCapacity());
//				
//				if (capacity > 0) {
//					ParkingFacility parkingFacility = new ParkingFacility(facility.getId(), facility.getLinkId(), "garageParking", capacity);
//					parkingFacilities.put(facility.getId(), parkingFacility);			
//				}
//			}
		}
		
		// Create a quadtree containing all parking facilities
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ParkingFacility parkingFacility : parkingFacilities.values()) {
			ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(parkingFacility.getFaciltyId());
			if (facility.getCoord().getX() < minx) { minx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() < miny) { miny = facility.getCoord().getY(); }
			if (facility.getCoord().getX() > maxx) { maxx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() > maxy) { maxy = facility.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		allParkingFacilities = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		availableParkingFacilities = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		fillQuadTree();
	}

	private void fillQuadTree() {
		
		this.availableParkingFacilities.clear();
		
		for (ParkingFacility parkingFacility : parkingFacilities.values()) {
			ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(parkingFacility.getFaciltyId());
				
			// add the facility to the quadtrees
			allParkingFacilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			availableParkingFacilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			
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
	
	public int getFreeCapacity(Id facilityId) {
		int freeCapacity = this.parkingFacilities.get(facilityId).getFreeCapacity();

		if (freeCapacity < 0) throw new RuntimeException("Free capacity < 0 was found for facility " + 
				facilityId.toString() + ": " + freeCapacity);
		
		return freeCapacity;
	}
	
	public boolean reserveParking(Id vehicleId, Id facilityId) {
		
		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);
				
		boolean reserved = parkingFacility.reserve(vehicleId);
		
		if (getFreeCapacity(facilityId) == 0) {
			markFacilityAsFull(facilityId);
		}
		
		if (!reserved) throw new RuntimeException("Could not reserve parking for vehicle " + vehicleId.toString() +
				" at parking facility " + facilityId.toString());
		
		return reserved;
	}
	
	public boolean unReserveParking(Id vehicleId, Id facilityId) {
		
		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);
		return parkingFacility.unReserve(vehicleId);
	}
	
	public boolean parkVehicle(Id vehicleId, Id facilityId) {
		
		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);
		boolean parked = parkingFacility.reservedToOccupied(vehicleId);
		
		if (!parked) throw new RuntimeException("Could not park vehicle " + vehicleId.toString() +
				" at parking facility " + facilityId.toString());
		
		return parked;
	}
	
	public void unParkVehicle(Id vehicleId, Id facilityId) {
		
		ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);
		boolean unparked = parkingFacility.release(vehicleId);

		if (!unparked) throw new RuntimeException("Could not unpark vehicle " + vehicleId.toString() +
				" at parking facility  " + facilityId.toString());
			
		if (getFreeCapacity(facilityId) == 1) {
			markFacilityAsNonFull(facilityId);
		}
	}

	private void markFacilityAsFull(Id facilityId) {
		ActivityFacility activityFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
		this.availableParkingFacilities.remove(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(), activityFacility);
	}
	
	private void markFacilityAsNonFull(Id facilityId) {
		ActivityFacility activityFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
		this.availableParkingFacilities.put(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(), activityFacility);
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
				if (parkingFacility.getFreeCapacity() > 0) parkings.add(parkingId);
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
	 * So far, use this simple approach. Later use a lookup map or something similar.
	 */
	public Id getVehicleId(Person person) {
		return person.getId();
	}
	
	public static class ParkingFacility {
		
		private final Id facilityId;
		private final Id linkId;
		private final String type;
		private final int capacity;
		private Set<Id> reserved = new LinkedHashSet<Id>();
		private Set<Id> occupied = new LinkedHashSet<Id>();
		
		public ParkingFacility(Id facilityId, Id linkId, String type, int capacity) {
			this.facilityId = facilityId;
			this.linkId = linkId;
			this.type = type;
			this.capacity = capacity;
		}
		
		public Id getFaciltyId() {
			return this.facilityId;
		}
		
		public Id getLinkId() {
			return this.linkId;
		}
		
		public String getParkingType() {
			return this.type;
		}
		
		public boolean reservedToOccupied(Id id) {
			if (reserved.remove(id)) {
				return occupied.add(id);
			} else return false;
			
		}
		
		public boolean release(Id id) {
			return this.occupied.remove(id);
		}
		
		public boolean reserve(Id id) {
			if (reserved.size() + occupied.size() < capacity) {
				return reserved.add(id);
			} else return false;
		}
				
		public boolean unReserve(Id id) {
			return reserved.remove(id);
		}
		
		public int getFreeCapacity() {
			return this.capacity - (reserved.size() + occupied.size());
		}
		
		public void reset() {
			
			if (this.reserved.size() > 0) log.warn("Found parking spots which are still reserved at the end of the simulation!");
			
			this.reserved.clear();
			this.occupied.clear();
		}
		
		public int getCapacity(){
			return this.capacity;
		}
	}
	
}
