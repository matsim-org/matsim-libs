/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveFacilitiesFromZH.java
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

package playground.christoph.energyflows.facilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.utils.misc.Counter;

public class RemoveFacilitiesFromZH {
	
	public List<ActivityFacility> removeFacilities(ActivityFacilitiesImpl facilities, Set<Id> facilitiesToRemove) {
		
		List<ActivityFacility> removedFacilities = new ArrayList<ActivityFacility>();
		Counter counter = new Counter("removed facilities: ");
		for (Id id : facilitiesToRemove) {
			ActivityFacility removedFacility = facilities.getFacilities().remove(id);
			if (removedFacility != null) {
				removedFacilities.add(removedFacility);
				counter.incCounter();
			}
		}
		counter.printCounter();
		return removedFacilities;
	}
}
