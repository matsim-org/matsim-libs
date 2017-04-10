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

package playground.jbischoff.sharedTaxiBerlin.preparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 * This is an example script to create (robo)taxi vehicle files. The vehicles are distributed randomly in the network.
 *
 */
public class CreateSharedTaxiVehicles {
	static int maxY = 5823460;
	static int minY = 5814023;
	static int minX = 388558;
	static int maxX = 396548;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		double operationStartTime = 17*3600; //t0
		double operationEndTime = 31*3600.;	//t1
		int seats = 6;
		String networkfile = "../../../shared-svn/projects/bvg_sharedTaxi/input/network-bvg_25833_cut_cleaned.xml.gz";
		List<Vehicle> vehicles = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		List<Id<Link>> allLinks = new ArrayList<>();
		allLinks.addAll(scenario.getNetwork().getLinks().keySet());
		for (int numberofVehicles = 25; numberofVehicles<=500; numberofVehicles+=25){
		
		String taxisFile = "../../../shared-svn/projects/bvg_sharedTaxi/input/vehicles_net_bvg/cap_"+seats+"/taxis_"+numberofVehicles+".xml.gz";
		for (int i = 0; i< numberofVehicles;i++){
			Link startLink;
			do {
				double x = minX + random.nextInt(maxX-minX) ;
				double y = minY + random.nextInt(maxY-minY);
				Coord startCoord = new Coord(x,y) ;
				
				startLink =  NetworkUtils.getNearestLink(scenario.getNetwork(), startCoord);
			}
			while (!startLink.getAllowedModes().contains(TransportMode.car));
			//for multi-modal networks: Only links where cars can ride should be used.
			Vehicle v = new VehicleImpl(Id.create("taxi"+i, Vehicle.class), startLink, seats, operationStartTime, operationEndTime);
			
		    vehicles.add(v);    
			
		}
		new VehicleWriter(vehicles).write(taxisFile);
		}
	}

}
