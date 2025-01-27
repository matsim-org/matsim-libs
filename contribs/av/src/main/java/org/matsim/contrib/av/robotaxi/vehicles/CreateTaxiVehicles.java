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
package org.matsim.contrib.av.robotaxi.vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 * This is an example script to create (robo)taxi vehicle files. The vehicles are distributed randomly in the network.
 *
 */
public class CreateTaxiVehicles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		int numberofVehicles = 5000;
		double operationStartTime = 0.; //t0
		double operationEndTime = 24*3600.;	//t1
		int seats = 4;
		String networkfile = "cottbus_robotaxi/network.xml.gz";
		String taxisFile = "./src/main/resources/cottbus_robotaxi/taxis_"+numberofVehicles+".xml";
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		List<Id<Link>> allLinks = new ArrayList<>(scenario.getNetwork().getLinks().keySet());
		for (int i = 0; i< numberofVehicles;i++){
			Link startLink;
			do {
			Id<Link> linkId = allLinks.get(random.nextInt(allLinks.size()));
			startLink =  scenario.getNetwork().getLinks().get(linkId);
			}
			while (!startLink.getAllowedModes().contains(TransportMode.car));
			//for multi-modal networks: Only links where cars can ride should be used.
			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create("taxi" + i, DvrpVehicle.class))
					.startLinkId(startLink.getId())
					.capacity(seats)
					.serviceBeginTime(operationStartTime)
					.serviceEndTime(operationEndTime)
					.build());

		}
		new FleetWriter(vehicles.stream(), new IntegerLoadType("passengers")).write(taxisFile);
	}

}
