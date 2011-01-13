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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;

public class PrintInfoAboutFacilitiesOnALink {

	public static void main(String[] args) {
		
		String basePath="H:/data/cvs/ivt/studies/switzerland/";
		String plansFile = "H:/data/experiments/ARTEMIS/input/plans_census2000v2_zrhCutC_1pml.xml.gz";
		String networkFile = basePath + "networks/teleatlas-ivtcheu-zrhCutC/network.xml.gz";
		String facilititiesPath = basePath + "facilities/facilities.zrhCutC.xml.gz";
		
		Id idOfLinkForWhichFacilitiesShouldBePrinted=new IdImpl("17560001856956FT");
		
		
		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);

		NetworkImpl network=scenario.getNetwork();
		ActivityFacilitiesImpl facilities=scenario.getActivityFacilities();
				
		for (Id facilityId:facilities.getFacilities().keySet()){
			ActivityFacilityImpl facility=(ActivityFacilityImpl) facilities.getFacilities().get(facilityId);
			Id linkOfFacility=network.getNearestLink(facility.getCoord()).getId();
			
			if (linkOfFacility.equals(idOfLinkForWhichFacilitiesShouldBePrinted)){
				FacilityLib.printActivityFacilityImpl(facility);
			}
		}
		
	}
	
}
