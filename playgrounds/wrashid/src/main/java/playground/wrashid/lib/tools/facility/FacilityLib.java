/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityLib.java
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

import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;

public class FacilityLib {

	public static void printActivityFacilityImpl(ActivityFacilityImpl facility){
//		System.out.print("id: ");
//		System.out.print(facility.getId());
//		System.out.print(", coord: ");
//		System.out.print(facility.getCoord());
//		System.out.print(", desc: ");
//		System.out.print(facility.getDesc());
//		System.out.print(", actOptions: ");
//		
//		for (String option:facility.getActivityOptions().keySet()){
//			System.out.print("[" + facility.getActivityOptions().get(option) + "]");
//		}
//		System.out.println();
		System.out.print(getActivityFacilityImplString(facility));
	}
	
	public static String getActivityFacilityImplString(ActivityFacilityImpl facility){
		String result="";
		result+="id: ";
		result+=facility.getId();
		result+=", coord: ";
		result+=facility.getCoord();
		result+=", desc: ";
		result+=facility.getDesc();
		result+=", actOptions: ";
		
		for (String option:facility.getActivityOptions().keySet()){
			result+="[" + facility.getActivityOptions().get(option) + "]";
		}
		result+="\n";
		return result;
	}
	
	public static String getActivityFacilityImplStringForKml(ActivityFacilityImpl facility){
	
		return getActivityFacilityImplString(facility);
	}
	
	public static double getTotalCapacityOfFacility(ActivityFacilityImpl activityFacility){
		double totalFacilityCapacity=0;
		for (ActivityOption actOption:activityFacility.getActivityOptions().values()){
			totalFacilityCapacity+=actOption.getCapacity();
		}
		return totalFacilityCapacity;
	}
	
}
