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
package wobscenario.peoplemover;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.file.FleetWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 * This is an example script to create (robo)taxi vehicle files. The vehicles are distributed randomly in the network.
 *
 */
public class CreatePeopleMoverVehicles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		int numberofVehicles = 300;
		double operationStartTime = 0.; //t0
		double operationEndTime = 36*3600.;	//t1
		int seats = 8;
		String networkfile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/networkpt-av-mar17.xml";
		String taxisFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/peoplemover/testscenario/pm_"+numberofVehicles+".xml";
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		List<Id<Link>> allLinks = new ArrayList<>();
		allLinks.addAll(scenario.getNetwork().getLinks().keySet());
		for (int i = 0; i< numberofVehicles;i++){
			Link startLink;
			do {
			Id<Link> linkId = allLinks.get(random.nextInt(allLinks.size()));
			startLink =  scenario.getNetwork().getLinks().get(linkId);
			}
			while (!startLink.getAllowedModes().contains("av"));
			//for multi-modal networks: Only links where cars can ride should be used.
			DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create("taxi" + i, Vehicle.class))
					.startLinkId(startLink.getId())
					.capacity(seats)
					.serviceBeginTime(operationStartTime)
					.serviceEndTime(operationEndTime)
					.build();
		    vehicles.add(v);    
			
		}
		new FleetWriter(vehicles.stream()).write(taxisFile);
	}

}
