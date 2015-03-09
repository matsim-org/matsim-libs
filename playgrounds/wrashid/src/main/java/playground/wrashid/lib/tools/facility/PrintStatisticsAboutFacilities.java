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

package playground.wrashid.lib.tools.facility;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilityImpl;


public class PrintStatisticsAboutFacilities {

	public static void main(String[] args) {
		String facilitiesPath = "C:/data/parkingSearch/zurich/input/facilities.xml.gz";
		ActivityFacilities facilities = GeneralLib.readActivityFacilities(facilitiesPath);

		IntegerValueHashMap<String> actTypes=new IntegerValueHashMap<String>();

		for (Id facilityId : facilities.getFacilities().keySet()) {
			ActivityFacilityImpl facility = (ActivityFacilityImpl) facilities.getFacilities().get(facilityId);

			for (String activityOption : facility.getActivityOptions().keySet()) {
				actTypes.increment(activityOption);
			}

		}
		
		for (String activityType:actTypes.getKeySet()){
			System.out.println(activityType + " => " + actTypes.get(activityType));
		}

	}

}
