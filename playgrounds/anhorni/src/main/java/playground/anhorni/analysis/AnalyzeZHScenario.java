/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.analysis;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class AnalyzeZHScenario {
	
	private final static Logger log = Logger.getLogger(AnalyzeZHScenario.class);
	private static String path = "src/main/java/playground/anhorni/";
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String networkfilePath = path + "input/ZH/network.xml";
	private String facilitiesfilePath = path + "input/ZH/facilities.xml.gz";
	

	public static void main(final String[] args) {
		AnalyzeZHScenario analyzer = new AnalyzeZHScenario();
		analyzer.run();
		log.info("Analysis finished -----------------------------------------");
	}
	
	public void run() {
		new MatsimNetworkReader(scenario).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		
		this.analyzefacilities(this.scenario.getActivityFacilities().getFacilities().keySet(), "Switzerland");
		this.analyzefacilities(getZHfacilityIds(), "ZH");
	}
	
	private TreeSet<Id<ActivityFacility>> getZHfacilityIds() {
		TreeSet<Id<ActivityFacility>> zhFacilityIds = new TreeSet<Id<ActivityFacility>>();
		NodeImpl centerNode = (NodeImpl) this.scenario.getNetwork().getNodes().get(Id.create("2531", Node.class));
		double radius = 30000;
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			if (((CoordImpl)centerNode.getCoord()).calcDistance(facility.getCoord()) < radius) {
				zhFacilityIds.add(facility.getId());
			}
		}
		return zhFacilityIds;
	}
	
	private void analyzefacilities(Set<Id<ActivityFacility>> set, String region) {
		log.info("Number of " + region + " facilities: " + set.size());
		int numberOfActivityOptions = 0;
		int numberOfShopFacilities = 0;
		int numberOfLeisureFacilities = 0;
		for (Id<ActivityFacility> facilityId: set) {
			ActivityFacility facility = this.scenario.getActivityFacilities().getFacilities().get(facilityId);
			numberOfActivityOptions += facility.getActivityOptions().entrySet().size();
			
			if (facility.getActivityOptions().containsKey("s")) numberOfShopFacilities++;
			if (facility.getActivityOptions().containsKey("l")) numberOfLeisureFacilities++;
		}
		log.info("Number of " + region + " shopping facilities: " + numberOfShopFacilities);
		log.info("Number of " + region + " leisure facilities: " + numberOfLeisureFacilities);		
		log.info("Number of " + region + " activity options: " + numberOfActivityOptions);
	}
}
