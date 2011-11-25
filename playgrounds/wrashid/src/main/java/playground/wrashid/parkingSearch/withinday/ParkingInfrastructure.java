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
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.obj.IntegerValueHashMap;

public class ParkingInfrastructure {

	private final QuadTree<Id> parkingFacilities;
	private final Map<Id, List<Id>> parkingFacilitiesOnLinkMapping; // <LinkId,
																	// List<FacilityId>>
	private final Map<Id, Id> facilityToLinkMapping; // <FacilityId, LinkId>
	private final IntegerValueHashMap<Id> facilityCapacities;

	public ParkingInfrastructure(Scenario scenario) {
		facilityCapacities = new IntegerValueHashMap<Id>();
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
		
		parkingFacilities = new QuadTree<Id>(minx, miny, maxx, maxy);
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			
			// if the facility offers a parking activity
			if (facility.getActivityOptions().containsKey("parking")) {
				
				// add the facility to the quadtree
				parkingFacilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility.getId());
				
				// add the facility to the facilitiesOnLinkMapping
				List<Id> list = parkingFacilitiesOnLinkMapping.get(facility.getLinkId());
				if (list == null) {
					list = new ArrayList<Id>();
					parkingFacilitiesOnLinkMapping.put(facility.getLinkId(), list);
				}
				list.add(facility.getId());
				
				// add the facility to the facilityToLinkMapping
				facilityToLinkMapping.put(facility.getId(), facility.getLinkId());
				
				// set initial capacity
				facilityCapacities.incrementBy(facility.getId(), 100);
			}
		}
	}

	public int getFreeCapacity(Id facilityId) {
		return facilityCapacities.get(facilityId);
	}

	public void parkVehicle(Id facilityId) {
		facilityCapacities.decrement(facilityId);
	}

	public void unParkVehicle(Id facilityId) {
		facilityCapacities.increment(facilityId);
	}

	public List<Id> getParkingsOnLink(Id linkId) {
		return parkingFacilitiesOnLinkMapping.get(linkId);
	}

	public Id getClosestFacilityFromCoord(Coord coord) {
		return parkingFacilities.get(coord.getX(), coord.getY());
	}

}
