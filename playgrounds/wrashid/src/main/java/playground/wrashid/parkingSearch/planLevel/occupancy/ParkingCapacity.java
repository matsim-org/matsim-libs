/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

public class ParkingCapacity {

	/*
	 * id: facilityId value: capacity of the facility
	 */
	private HashMap<Id<ActivityFacility>, Integer> facilityCapacity = new HashMap<>();
	private ArrayList<ActivityFacility> parkingFacilities=new ArrayList<ActivityFacility>();

	public ArrayList<ActivityFacility> getParkingFacilities() {
		return parkingFacilities;
	}

	public ParkingCapacity(ActivityFacilities facilities) {

		for (ActivityFacility facility : facilities.getFacilities().values()) {

			for (ActivityOption activityOption : facility.getActivityOptions().values()) {

				if (activityOption.getType().equalsIgnoreCase("parking")) {
					facilityCapacity.put(facility.getId(), (int) Math.round(Math.floor(activityOption.getCapacity())));
					parkingFacilities.add(facility);
				}

			}

		}

	}

	public int getParkingCapacity(Id<ActivityFacility> facilityId) {
		return facilityCapacity.get(facilityId);
	}
	
	public int getNumberOfParkings(){
		return facilityCapacity.size();
	}
	
	 

}
