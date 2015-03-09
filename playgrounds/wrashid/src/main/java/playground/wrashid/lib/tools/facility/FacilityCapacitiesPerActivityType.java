/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityCapacitiesPerFacilityType.java
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

package playground.wrashid.lib.tools.facility;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilityImpl;

public class FacilityCapacitiesPerActivityType {

	public static void main(String[] args) {
		String facilitiesPath = "E:/svn/studies/switzerland/facilities/facilities.zrhCutC.xml.gz";

		ActivityFacilities facilities = GeneralLib.readActivityFacilities(facilitiesPath);
		
		DoubleValueHashMap<String> activityTypeCapacities=new DoubleValueHashMap<String>();
		
		for (Id facilityId:facilities.getFacilities().keySet()){
			ActivityFacilityImpl facility=(ActivityFacilityImpl) facilities.getFacilities().get(facilityId);
			
			for (String actType:facility.getActivityOptions().keySet()){
				activityTypeCapacities.incrementBy(actType, facility.getActivityOptions().get(actType).getCapacity());
			}
			
		}
		
		System.out.println("capacitities available at facilities for different types of activities: ");
		activityTypeCapacities.printToConsole();
		
	}
	
}
