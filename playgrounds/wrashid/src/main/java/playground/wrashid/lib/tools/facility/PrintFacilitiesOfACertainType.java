/* *********************************************************************** *
 * project: org.matsim.*
 * PrintFacilitiesOfACertainType.java
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class PrintFacilitiesOfACertainType {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String basePath="H:/data/cvs/ivt/studies/switzerland/";
		String facilititiesPath = basePath + "facilities/facilities.zrhCutC.xml.gz";
		String activityTypeFilter="education_higher";
		String outputKmlFile="H:/data/experiments/ARTEMIS/zh/dumb charging/output/analysis/higherEduFacilities.kml";
		
		ActivityFacilitiesImpl facilities = GeneralLib.readActivityFacilities(facilititiesPath);
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		
		for (Id facilityId:facilities.getFacilities().keySet()){
			ActivityFacilityImpl facility=(ActivityFacilityImpl) facilities.getFacilities().get(facilityId);
			
			if (facility.getActivityOptions().containsKey(activityTypeFilter)){
				FacilityLib.printActivityFacilityImpl(facility);
				basicPointVisualizer.addPointCoordinate(facility.getCoord(), FacilityLib.getActivityFacilityImplStringForKml(facility) ,Color.GREEN);
			}
		}
		
		basicPointVisualizer.write(outputKmlFile);
		
	}

}
