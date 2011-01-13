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
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class FacilityCapacitiesPerActivityType {

	public static void main(String[] args) {
		String basePath="H:/data/cvs/ivt/studies/switzerland/";
		String facilititiesPath = basePath + "facilities/facilities.zrhCutC.xml.gz";

		ActivityFacilitiesImpl facilities = GeneralLib.readActivityFacilities(facilititiesPath);
		
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
