/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingInfrastructure.java
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

package playground.wrashid.parkingSearch.withinday;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;

public class ParkingInfrastructure implements ActivityStartEventHandler, ActivityEndEventHandler {

	private final QuadTree<ActivityFacility> parkingFacilities;
	private final Map<Id, List<Id>> parkingFacilitiesOnLinkMapping; // <LinkId, List<FacilityId>>
	private final Map<Id, Id> facilityToLinkMapping;	// <FacilityId, LinkId>
	private final IntegerValueHashMap<Id> reservedCapcities;	// number of reserved parkings
	private final IntegerValueHashMap<Id> facilityCapacities;	// remaining capacity
	private final HashMap<String, HashSet<Id>> parkingTypes;
	private final ParkingCostCalculator parkingCostCalculator;
	
	public ParkingInfrastructure(Scenario scenario, HashMap<String, HashSet<Id>> parkingTypes, ParkingCostCalculator parkingCostCalculator) {
		this.parkingCostCalculator = parkingCostCalculator;
		facilityCapacities = new IntegerValueHashMap<Id>();
		reservedCapcities = new IntegerValueHashMap<Id>();
		facilityToLinkMapping = new HashMap<Id, Id>();
		parkingFacilitiesOnLinkMapping = new HashMap<Id, List<Id>>();
		
		// Create a quadtree containing all parking facilities
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			if (facility.getCoord().getX() < minx) { minx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() < miny) { miny = facility.getCoord().getY(); }
			if (facility.getCoord().getX() > maxx) { maxx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() > maxy) { maxy = facility.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		parkingFacilities = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			
			// if the facility offers a parking activity
			if (facility.getActivityOptions().containsKey("parking")) {
				
				// add the facility to the quadtree
				parkingFacilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
				
				// add the facility to the facilitiesOnLinkMapping
				List<Id> list = parkingFacilitiesOnLinkMapping.get(facility.getLinkId());
				if (list == null) {
					list = new ArrayList<Id>();
					parkingFacilitiesOnLinkMapping.put(facility.getLinkId(), list);
				}
				list.add(facility.getId());
				
				// add the facility to the facilityToLinkMapping
				facilityToLinkMapping.put(facility.getId(), facility.getLinkId());
			}
		}
		
		this.parkingTypes=parkingTypes;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		/*
		if (event.getActType().equals("parking")) {
			reservedCapcities.decrement(event.getFacilityId());
			facilityCapacities.increment(event.getFacilityId());
		}
		*/
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		/*
		if (event.getActType().equals("parking")) {
			facilityCapacities.increment(event.getFacilityId());
		}
		*/
	}
	
	public int getFreeCapacity(Id facilityId) {
		return facilityCapacities.get(facilityId);
	}

	public void parkVehicle(Id facilityId) {
		reservedCapcities.decrement(facilityId);
	}

	public void unParkVehicle(Id facilityId) {
		reservedCapcities.increment(facilityId);
	}
	


	public List<Id> getParkingsOnLink(Id linkId) {
		return parkingFacilitiesOnLinkMapping.get(linkId);
	}

	public Id getFreeParkingFacilityOnLink(Id linkId, String parkingType) {
		HashSet<Id> parkings=null;
		if (parkingTypes!=null){
			parkings = parkingTypes.get(parkingType);
		}
		
		List<Id> list = getParkingsOnLink(linkId);
		if (list == null) return null;
		else {
			int maxCapacity = 0;
			Id facilityId = null;
			for (Id id : list) {
				if (parkings!=null && !parkings.contains(id)){
					continue;
				}
				
				int capacity = facilityCapacities.get(id);
				int reserved = reservedCapcities.get(id);
				if ((capacity - reserved) > maxCapacity) facilityId = id;
			}
			return facilityId;
		}
	}
	
	public Id getClosestFreeParkingFacility(Coord coord) {
		LinkedList<ActivityFacility> tmpList=new LinkedList<ActivityFacility>();
		ActivityFacility parkingFacility=parkingFacilities.get(coord.getX(), coord.getY());
		
		// if parking full, try finding other free parkings in the quadtree
		while (facilityCapacities.get(parkingFacility.getId())<=0){
			removeFullParkingFromQuadTree(tmpList, parkingFacility);
			parkingFacility=parkingFacilities.get(coord.getX(), coord.getY());
		}
		
		resetParkingFacilitiesQuadTree(tmpList);
		
		return parkingFacility.getId();
	}

	private void removeFullParkingFromQuadTree(LinkedList<ActivityFacility> tmpList, ActivityFacility parkingFacility) {
		tmpList.add(parkingFacility);
		parkingFacilities.remove(parkingFacility.getCoord().getX(), parkingFacility.getCoord().getX(), parkingFacility);
	}

	private void resetParkingFacilitiesQuadTree(LinkedList<ActivityFacility> tmpList) {
		for (ActivityFacility parking:tmpList){
			parkingFacilities.put(parking.getCoord().getX(), parking.getCoord().getX(), parking);
		}
	}

	@Override
	public void reset(int iteration) {
		
		for (Id facilityId : facilityToLinkMapping.keySet()) {
			// set initial capacity
			facilityCapacities.set(facilityId, 1000);
			reservedCapcities.set(facilityId, 0);
		}
	}

	public ParkingCostCalculator getParkingCostCalculator() {
		return parkingCostCalculator;
	}

}
