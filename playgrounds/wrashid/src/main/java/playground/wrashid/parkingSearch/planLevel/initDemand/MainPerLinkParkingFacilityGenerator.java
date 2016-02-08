/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.parkingSearch.planLevel.initDemand;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;

/**
 * Add parking facilities to each road according to their length (at least on per road).
 * 
 * @author rashid_waraich
 * 
 */

public class MainPerLinkParkingFacilityGenerator {

	static HashMap<String, Integer> hm = new HashMap<String, Integer>();
	static int numberOfAgents = 0;

	public static void main(String[] args) {

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String networkFile = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\network.xml.gz";
		String facilitiesPath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\parkingFacilities.xml.gz";

		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
		Network net = sc.getNetwork();

		ActivityFacilitiesImpl activityFacilities = new ActivityFacilitiesImpl();

		int parkPlatzId = 1;
		int totalNumberOfParkingsAdded=0;
		for (Link link : net.getLinks().values()) {
			// 5m long car, half of the street available for parking
			// don't do this change - will be done later probably
			int parkingCapacity = (int) Math.round(Math.ceil(link.getLength() / 2.0 / 5.0/100.0/2));
			totalNumberOfParkingsAdded+=parkingCapacity;
			ActivityFacilityImpl activityFacility = activityFacilities.createAndAddFacility(Id.create(parkPlatzId, ActivityFacility.class), link.getCoord());
			activityFacility.createAndAddActivityOption("parking").setCapacity(parkingCapacity);
			parkPlatzId++;
		}
		
		System.out.println("total number of parking facilities added: " + totalNumberOfParkingsAdded);

		FacilitiesWriter fw = new FacilitiesWriter(activityFacilities);
		fw.write(facilitiesPath);
	}

}
