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

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class VisualizeFacilitiesOfACertainType {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String facilititiesPath = "H:/data/experiments/ARTEMIS/output/run10/output_facilities.xml.gz";
		String activityTypeFilter=null; 
		double scalingPercentage=0.01; // 1.0 means 100%
		String outputKmlFile=GeneralLib.eclipseLocalTempPath + "/facilities.kml";
		
		ActivityFacilitiesImpl facilities = GeneralLib.readActivityFacilities(facilititiesPath);
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		Random rand=new Random();
		
		for (Id facilityId:facilities.getFacilities().keySet()){
			ActivityFacilityImpl facility=(ActivityFacilityImpl) facilities.getFacilities().get(facilityId);
			
			if (activityTypeFilter==null || facility.getActivityOptions().containsKey(activityTypeFilter)){
				//FacilityLib.printActivityFacilityImpl(facility);
				if (shouldBePartOfSampling(scalingPercentage, rand)){
					basicPointVisualizer.addPointCoordinate(facility.getCoord(), FacilityLib.getActivityFacilityImplStringForKml(facility) ,Color.GREEN);
				}
			}
		}
		
		basicPointVisualizer.write(outputKmlFile);
	}

	private static boolean shouldBePartOfSampling(double scalingPercentage, Random rand) {
		return rand.nextDouble()<scalingPercentage;
	}

}
