/* *********************************************************************** *
 * project: org.matsim.*
 * PrintInfoAboutFacilitiesOnALink.java
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
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;


public class PrintInfoAboutFacilitiesOnALink {

	public static void main(String[] args) {
		
		String basePath="H:/data/cvs/ivt/studies/switzerland/";
		String plansFile = "H:/data/experiments/ARTEMIS/input/plans_census2000v2_zrhCutC_1pml.xml.gz";
		String networkFile = basePath + "networks/teleatlas-ivtcheu-zrhCutC/network.xml.gz";
		String facilititiesPath = basePath + "facilities/facilities.zrhCutC.xml.gz";
		
		Id<Link> idOfLinkForWhichFacilitiesShouldBePrinted=Id.create("17560001856956FT", Link.class);
		
		
		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);

		NetworkImpl network=(NetworkImpl) scenario.getNetwork();
		ActivityFacilities facilities=scenario.getActivityFacilities();
				
		for (Id<ActivityFacility> facilityId:facilities.getFacilities().keySet()){
			ActivityFacilityImpl facility=(ActivityFacilityImpl) facilities.getFacilities().get(facilityId);
			Id<Link> linkOfFacility= NetworkUtils.getNearestLink(network, facility.getCoord()).getId();
			
			if (linkOfFacility.equals(idOfLinkForWhichFacilitiesShouldBePrinted)){
				FacilityLib.printActivityFacilityImpl(facility);
			}
		}
		
	}
	
}
