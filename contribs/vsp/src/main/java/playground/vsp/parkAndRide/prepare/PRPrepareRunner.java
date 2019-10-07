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
package playground.vsp.parkAndRide.prepare;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

/**
 * @author Ihab
 *
 */

public class PRPrepareRunner {
	
	// input files
	static String networkFile = "/Users/Ihab/Documents/workspace/shared-svn/studies/ihab/parkAndRide/input/test_network.xml";
	static String scheduleFile = "/Users/Ihab/Documents/workspace/shared-svn/studies/ihab/parkAndRide/input/scheduleFile.xml";
	static String vehiclesFile = "/Users/Ihab/Documents/workspace/shared-svn/studies/ihab/parkAndRide/input/vehiclesFile.xml";
	
//	// input files
//	static String networkFile = "input/network.xml";
//	static String scheduleFile = "input/transitSchedule.xml";
//	static String vehiclesFile = "input/transitVehicles.xml";
	
	// output files
	static String outputPath = "/Users/Ihab/Documents/workspace/shared-svn/studies/ihab/parkAndRide/input/";
	static String prFacilitiesFile = "prFacilityFile.csv";
	static String prNetworkFile = "prNetwork.xml";
	
	// settings
	
	// if true a park-and-ride input file is used to create park-and-ride facilities
	static boolean usePrInputFile = false;
	// the park-and-ride input file
	static String prInputFile = "input/prInputFile.csv";

	// if true the transit schedule is used to create park-and-ride facilities
	static boolean useScheduleFile = true;	
	// a constant capacity of each park-and-ride facility (maximum number of vehicles) 
	static int constantCapacity = 100000;
	
	static double extensionRadius = 10;
	static int maxSearchSteps = 100;
	
	// attributes of park-and-ride facility:
	private double linkCapacity = 2000;
	private double freeSpeed = 2.77778;
	private double length = 20;
	private double nrOfLanes = 40;
	
	private MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	public static void main(String[] args) throws IOException {
		
		PRPrepareRunner prGeneratorMain = new PRPrepareRunner();
		prGeneratorMain.run();
	}

	private void run() {
		
		loadScenario();
		
		PRFactory prFactory = new PRFactory(this.scenario, extensionRadius, maxSearchSteps, outputPath);
		
		if (usePrInputFile && useScheduleFile){
			throw new RuntimeException("usePRInputFile and useScheduleFile set true. Aborting...");
		}
		
		prFactory.setUseInputFile(usePrInputFile, prInputFile);
		prFactory.setUseScheduleFile(useScheduleFile, constantCapacity);
		
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
		config.transit().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		config.network().setInputFile(networkFile);
		ScenarioUtils.loadScenario(scenario);
	}
}
