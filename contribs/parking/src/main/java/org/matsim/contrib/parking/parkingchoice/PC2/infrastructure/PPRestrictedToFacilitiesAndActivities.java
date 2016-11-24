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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.parkingchoice.lib.obj.LinkedListValueHashMap;

public class PPRestrictedToFacilitiesAndActivities extends PublicParking implements PrivateParking {


	public PPRestrictedToFacilitiesAndActivities(Id id, int capacity, Coord coord, ParkingCostModel parkingCostModel,
			String groupName, LinkedListValueHashMap<Id, String> facilitiesActs) {
		super(id, capacity, coord, parkingCostModel, groupName);
		this.facilitiesActs=facilitiesActs;
	}

	private LinkedListValueHashMap<Id, String> facilitiesActs;

	public void PPRestrictedToIndividuals(LinkedListValueHashMap<Id,String> facilitiesActs){
		this.facilitiesActs = facilitiesActs;
	}
	
	@Override
	public boolean isAllowedToUseParking(Id personId, Id actFacilityId, String actType) {
		return facilitiesActs.getKeySet().contains(actFacilityId) && facilitiesActs.get(actFacilityId).contains(actType);
	}
}
