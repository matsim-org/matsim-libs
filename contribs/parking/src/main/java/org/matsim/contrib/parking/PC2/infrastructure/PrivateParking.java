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
package org.matsim.contrib.parking.PC2.infrastructure;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

public interface PrivateParking extends PC2Parking{

	public boolean isAllowedToUseParking(Id<Person> personId, Id<ActivityFacility> actFacilityId, String actType);
	
//	//allow restricting to single person, actType at facility or whole facility.
//	String restrictionType;
//	HashSet<Id> facilityIds;
//	LinkedListValueHashMap<Id> actTypes;
////Todo three types	
//	
//	public boolean isAllowedToUseParking(Id personId, Id actFacilityId, String actType){
//		if (restrictionType.equalsIgnoreCase("personId")){
//			
//		} else if (restrictionType.equalsIgnoreCase("facility")){
//			return facilityIds.contains(actFacilityId);
//		}else if (restrictionType.equalsIgnoreCase("actType")){
//			facilityIds.contains(actFacilityId);
//		} else {
//			DebugLib.stopSystemAndReportInconsistency();
//		}
//		
//		return ownerId==agentId;
//	}

}
