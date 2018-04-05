/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package vwExamples.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;

/**
 * @author  saxer
 * This is a script that drops vehicles uniformly over the network. Vehicles are only dropped on links that match with drtTag
 *
 */
public class CreatePeopleMoverVehicles {

	static double operationStartTime = 0.; //t0
	static double operationEndTime = 36*3600.;	//t1
	static int seats = 8;
	static File networkfile = new File("D:\\Axer\\MatsimDataStore\\WOB_BS_DRT\\WOB\\input\\network\\network_area_wob_withDRT_links.xml.gz");
	static String networkfolder = networkfile.getParent();
	static int increment = 10;
	static String drtTag = "drt";
	static int numberOfVehicles = 1000;

	public static void main(String[] args) {
		
		for (int i = 1; i <= Math.ceil(numberOfVehicles/increment) ; i++) {
			createVehicles(networkfile, i*increment);
		}
		
		

	}
	
	public static void createVehicles(File networkfile, int numberofVehicles) {
		
				
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		String drtFleetFile = networkfolder+"/../fleets/"+"fleet_"+String.valueOf(numberofVehicles)+".xml.gz";
		List<Vehicle> vehicles = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile.toString());
		List<Id<Link>> allLinks = new ArrayList<>();
		allLinks.addAll(scenario.getNetwork().getLinks().keySet());
		for (int i = 0; i< numberofVehicles;i++){
			Link startLink;
			do {
			Id<Link> linkId = allLinks.get(random.nextInt(allLinks.size()));
			startLink =  scenario.getNetwork().getLinks().get(linkId);
			}
			while (!startLink.getAllowedModes().contains(drtTag));
			//for multi-modal networks: Only links where cars can ride should be used.
			Vehicle v = new VehicleImpl(Id.create("drt"+i, Vehicle.class), startLink, seats, operationStartTime, operationEndTime);
		    vehicles.add(v);    
			
		}
		new VehicleWriter(vehicles).write(drtFleetFile);
		
	}

}
