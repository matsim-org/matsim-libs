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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Ihab
 *
 */

public class ParkAndRideGeneratorMain {
	
	static String networkFile = "/Users/Ihab/Desktop/Berlin/berlinNetwork.xml";
	static String scheduleFile = "/Users/Ihab/Desktop/Berlin/berlinTransitSchedule.xml";
	static String vehiclesFile = "/Users/Ihab/Desktop/Berlin/berlinTransitVehicles.xml";
	
	static boolean usePrInputFile = true; // uses a file to insert park-and-ride facilities
	static String prInputFile = "/Users/Ihab/Desktop/Berlin/prInputData_Berlin.csv";

	static boolean useScheduleFile = false; // uses the schedule to insert park-and-ride facilities
	static String filterType = "berlin"; // defines at which stops park-and-ride is inserted (possible: allTransitStops, berlin)
	static int constantCapacity = 100000;
	
	static double extensionRadius = 10;
	static int maxSearchSteps = 100;
	
	// parkAndRide Link Attributes:
	private double linkCapacity = 2000;
	private double freeSpeed = 2.77778;
	private double length = 20;
	private double nrOfLanes = 40;
	
	// outputFiles
	static String outputPath = "/Users/Ihab/Desktop/Berlin/";
	static String prFacilitiesFile = "prFacilities_Berlin.txt";
	static String prNetworkFile = "berlinNetwork_PR_Berlin.xml";
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	public static void main(String[] args) throws IOException {
		
		ParkAndRideGeneratorMain prGeneratorMain = new ParkAndRideGeneratorMain();
		prGeneratorMain.run();
	}

	private void run() {
		
		loadScenario();
		
		ParkAndRideFactory prFactory = new ParkAndRideFactory(this.scenario, extensionRadius, maxSearchSteps, outputPath);
		
		if (usePrInputFile && useScheduleFile){
			throw new RuntimeException("usePRInputFile and useScheduleFile set true. Aborting...");
		}
		
		prFactory.setUseInputFile(usePrInputFile, prInputFile);
		prFactory.setUseScheduleFile(useScheduleFile, filterType, constantCapacity);
		
		prFactory.setId2prCarLinkToNode();		
		
		PRFacilityCreator prFacilityCreator = new PRFacilityCreator(this.scenario);
		prFacilityCreator.setLinkCapacity(this.linkCapacity);
		prFacilityCreator.setFreeSpeed(this.freeSpeed);
		prFacilityCreator.setLength(this.length);
		prFacilityCreator.setNrOfLanes(this.nrOfLanes);
		
		prFactory.createPRLinks(prFacilityCreator);
		prFactory.writeNetwork(outputPath + prNetworkFile);
		prFactory.writePrFacilities(prFacilityCreator, outputPath + prFacilitiesFile);
		
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
