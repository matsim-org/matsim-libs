/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

/**
 * Creates a mixed network with public and private transport based on OSM and HAFAS data.
 *
 * @author boescpa
 */
public class OSM2MixedConvMain {

	public static void main(String[] args) {

		// **************** Preparations ****************
		// Get an empty network and an empty schedule:
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		Vehicles vehicles = scenario.getTransitVehicles();
		// Get resources:
		String osmFile = args[0];
		String hafasFolder = args[1];
		String vehicleFile = args[2];
		String outputMultimodalNetwork = args[3];
		String outputSchedule = args[4];
		String outputVehicles = args[5];

		// **************** Convert ****************
		OSM2MixedConverter converter = new OSM2MixedConverter(network, schedule, vehicles, osmFile, hafasFolder, vehicleFile);
		converter.convertOSM2MultimodalNetwork();
		converter.writeOutput(outputMultimodalNetwork, outputSchedule, outputVehicles);
	}

}
