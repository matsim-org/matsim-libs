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

public class PPRestrictedToIndividuals extends PublicParking implements PrivateParking {
	private HashSet<Id<Person>> personIds;

	public PPRestrictedToIndividuals(Id id, int capacity, Coord coord, ParkingCostModel parkingCostModel, String groupName, HashSet<Id<Person>> personIds) {
		super(id, capacity, coord, parkingCostModel, groupName);
		this.personIds=personIds;
	}


	public void PPRestrictedToIndividuals(HashSet<Id<Person>> personIds){
		this.personIds = personIds;
	}
	
	@Override
	public boolean isAllowedToUseParking(Id personId, Id actFacilityId, String actType) {
		return personIds.contains(personId);
	}

	

}
