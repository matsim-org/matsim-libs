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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author Ihab
 *
 */

public class NetworkAddPRMain {
	
	// input
	static String networkFile = "../../shared-svn/studies/ihab/parkAndRide/input/testScenario_network.xml";
	static String scheduleFile = "../../shared-svn/studies/ihab/parkAndRide/input/testScenario_schedule.xml";
	static String vehiclesFile = "../../shared-svn/studies/ihab/parkAndRide/input/testScenario_vehicles.xml";
	
	// output
	static String prFacilitiesFile = "../../shared-svn/studies/ihab/parkAndRide/input/testScenario_PRfacilities.txt";
	static String prNetworkFile = "../../shared-svn/studies/ihab/parkAndRide/input/testScenario_PRnetwork.xml";
	
	private double extensionRadius = 100;
	private int maxSearchSteps = 50;
	
	// parkAndRide Link:
	private double capacity = 2000;
	private double freeSpeed = 2.77778;
	private double length = 20;
	private double nrOfLanes = 40;
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	public static void main(String[] args) {
		NetworkAddPRMain addParkAndRide = new NetworkAddPRMain();
		addParkAndRide.run();
	}

	private void run() {
		
		loadScenario();
		
		PRNodeSearch prNodeSearch = new PRNodeSearch();
		prNodeSearch.searchForCarLink(this.scenario, this.extensionRadius, this.maxSearchSteps);
		
		PRFacilityCreator prFacilityCreator = new PRFacilityCreator();
		prFacilityCreator.setCapacity(this.capacity);
		prFacilityCreator.setFreeSpeed(this.freeSpeed);
		prFacilityCreator.setLength(this.length);
		prFacilityCreator.setNrOfLanes(this.nrOfLanes);
		int i = 0;
		for (Node node : prNodeSearch.getCarLinkToNodes()){
			Id id = new IdImpl(i);
			prFacilityCreator.createPRFacility(id, node, this.scenario);
			i++;
		}
		
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		networkWriter.write(prNetworkFile);	
		
		PRFacilitiesWriter prFacilityWriter = new PRFacilitiesWriter();	
		prFacilityWriter.write(prFacilityCreator.getParkAndRideFacilities(), prFacilitiesFile);
		
		for (TransitStopFacility stop : prNodeSearch.getTransitStop2nearestCarLink().keySet()){
			System.out.println("TranistStopFacility: " + stop.getId().toString() + " " + stop.getCoord().toString() + " / next car-Link: " + prNodeSearch.getTransitStop2nearestCarLink().get(stop).getId() + " / toNode:" + prNodeSearch.getTransitStop2nearestCarLink().get(stop).getToNode().getCoord().toString());
		}	
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
