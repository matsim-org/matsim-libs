/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.parkingchoice.PC2.infrastructure;

import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingCostModel;
import org.matsim.facilities.ActivityFacility;

// people performing activities at facilities, which are mentioned in HashSet facility Ids, can use this parking.
public class PPRestrictedToFacilities extends PublicParking implements PrivateParking {

	public PPRestrictedToFacilities(Id<PC2Parking> id, int capacity, Coord coord, ParkingCostModel parkingCostModel, String groupName, HashSet<Id<ActivityFacility>> facilityIds) {
		super(id, capacity, coord, parkingCostModel, groupName);
		this.setFacilityIds(facilityIds);
	}

	private void setFacilityIds(HashSet<Id<ActivityFacility>> facilityIds) {
		this.facilityIds=facilityIds;
	}

	private HashSet<Id<ActivityFacility>> facilityIds;

	public void PPRestrictedToIndividuals(HashSet<Id<ActivityFacility>> facilityIds){
		this.setFacilityIds(facilityIds);
	}
	
	@Override
	public boolean isAllowedToUseParking(Id<Person> personId, Id<ActivityFacility> actFacilityId, String actType) {
		return getFacilityIds().contains(actFacilityId);
	}

	public HashSet<Id<ActivityFacility>> getFacilityIds() {
		return facilityIds;
	}


}
