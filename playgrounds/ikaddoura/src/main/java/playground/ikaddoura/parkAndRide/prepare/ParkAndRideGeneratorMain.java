/* *********************************************************************** *
 * project: org.matsim.*
 * networkChange.java
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.prepare;

import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Ihab
 *
 */

public class ParkAndRideGeneratorMain {
	
	// input
	static String networkFile = "/Users/Ihab/Desktop/PR_test/network.xml";
	static String scheduleFile = "/Users/Ihab/Desktop/PR_test/schedule.xml";
	static String vehiclesFile = "/Users/Ihab/Desktop/PR_test/vehicles.xml";
	
	static boolean usePrInputFile = false;
	static String prInputFile = "/Users/Ihab/Desktop/PR_test/prInputData.txt";

	static String filterType = "berlin"; // possible: allTransitStops, berlin
	
	// outputFiles
	static String outputPath = "/Users/Ihab/Desktop/PR_test/";
	static String prFacilitiesFile = "prFacilities.txt";
	static String prNetworkFile = "network_PR.xml";
	
	static double extensionRadius = 10;
	static int maxSearchSteps = 100;
		     
	// parkAndRide Link:
	private double capacity = 2000;
	private double freeSpeed = 2.77778;
	private double length = 20;
	private double nrOfLanes = 40;
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	public static void main(String[] args) throws IOException {
		
		ParkAndRideGeneratorMain prGeneratorMain = new ParkAndRideGeneratorMain();
		prGeneratorMain.run();
	}

	private void run() {
		
		loadScenario();
		
		ParkAndRideFactory prFactory = new ParkAndRideFactory(this.scenario, extensionRadius, maxSearchSteps, outputPath);
		
		prFactory.setUseInputFile(usePrInputFile, prInputFile);
		prFactory.setFilterType(filterType);
		prFactory.setId2prCarLinkToNode();
		
		Map<Id, PRCarLinkToNode> id2prCarLinkToNode = prFactory.getId2prCarLinkToNode();
			
		PRFacilityCreator prFacilityCreator = new PRFacilityCreator();
		prFacilityCreator.setCapacity(this.capacity);
		prFacilityCreator.setFreeSpeed(this.freeSpeed);
		prFacilityCreator.setLength(this.length);
		prFacilityCreator.setNrOfLanes(this.nrOfLanes);
		
		int i = 0;
		for (Id nodeId : id2prCarLinkToNode.keySet()){
			Id id = new IdImpl(i);
			prFacilityCreator.createPRFacility(id, id2prCarLinkToNode.get(nodeId).getNode(), this.scenario, id2prCarLinkToNode.get(nodeId).getStopName());
			i++;
		}
				
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		networkWriter.write(outputPath + prNetworkFile);
		
		TextFileWriter writer = new TextFileWriter();	
		writer.write(prFacilityCreator.getParkAndRideFacilities(), outputPath + prFacilitiesFile);
		
	}

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		config.network().setInputFile(networkFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();		
	}
}
