/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingFacilityCreator.java
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

package playground.christoph.parking.core.facilities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;

/**
 * Creates parking facilities along links if no parking facilities are available in a scenario.
 * 
 * @author cdobler
 */
public class ParkingFacilityCreator {
	
	private static final double parkingLotLength = 5.0;
	
	// use given capacity
	public static void createParkings(Scenario scenario, Link link, String parkingType, double capacity) {
		
		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		
		ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(link.getId());
		
		if (facility == null) {
			facility = factory.createActivityFacility(Id.create(link.getId().toString(), ActivityFacility.class), link.getCoord());
			((ActivityFacilityImpl) facility).setLinkId(link.getId());
			scenario.getActivityFacilities().addActivityFacility(facility);
		}
		ActivityOption activityOption = factory.createActivityOption(parkingType);
		activityOption.setCapacity(capacity);
		facility.addActivityOption(activityOption);
	}
	
	// calculate capacity based on link's length
	public static void createParkings(Scenario scenario, Link link, String parkingType) {		
		double capacity = Math.max(1.0, link.getLength() / parkingLotLength);
		createParkings(scenario, link, parkingType, capacity);
	}
	
	public static void createParkings(Scenario scenario, String parkingType) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			createParkings(scenario, link, parkingType);
		}
	}
	
	public static void createParkings(Scenario scenario, String parkingType, double capacity) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			createParkings(scenario, link, parkingType, capacity);
		}
	}
}