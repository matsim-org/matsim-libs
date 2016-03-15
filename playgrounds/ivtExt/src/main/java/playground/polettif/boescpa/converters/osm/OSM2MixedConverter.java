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

package playground.polettif.boescpa.converters.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import playground.polettif.boescpa.converters.osm.networkCreator.*;
import playground.polettif.boescpa.converters.osm.scheduleCreator.*;
import playground.polettif.boescpa.converters.osm.ptMapping.*;
import playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator.PTScheduleCreatorHAFAS;

/**
 * Provides the main body for the OSM to multimodal network conversion.
 *
 * @author boescpa
 */
public class OSM2MixedConverter {
	private static Logger log = Logger.getLogger(OSM2MixedConverter.class);

	private static CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");

	private static Network network;
	private static TransitSchedule schedule;
	private static Vehicles vehicles;

	public static void main(String[] args) {

		// **************** Preparations ****************
		// Get an empty network and an empty schedule:
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		network = scenario.getNetwork();
		schedule = scenario.getTransitSchedule();
		vehicles = scenario.getTransitVehicles();
		// Get resources:
		String path2InputFiles = args[0];
		String path2OSMFile = args[1];
		String outputMultimodalNetwork = args[2];
		String outputSchedule = args[3];
		String outputVehicles = args[4];

		// **************** Convert ****************
		convertOSM2MultimodalNetwork(path2InputFiles, path2OSMFile);
		writeOutput(outputMultimodalNetwork, outputSchedule, outputVehicles);
	}

	/**
	 * Converts a given OSM network to a multimodal MATSim network with the help of a HAFAS schedule.
	 * @param path2InputFiles
	 * @param path2OSMFile
	 */
	public static void convertOSM2MultimodalNetwork(String path2InputFiles, String path2OSMFile) {
		log.info("Conversion from OSM to multimodal MATSim network...");
		new MultimodalNetworkCreatorPT(network).createMultimodalNetwork(path2OSMFile);
		new PTScheduleCreatorHAFAS(schedule, vehicles, transformation).createSchedule(path2InputFiles);
		new PTMapperBusAndTram(schedule).routePTLines(network);
		log.info("Conversion from OSM to multimodal MATSim network... done.");
	}

	/**
	 * Writes the network and the schedule to the specified files.
	 *
	 * @param outputMultimodalNetwork	A string specifying the target network file.
	 * @param outputSchedule			A string specifying the target schedule file.
	 */
	public static void writeOutput(String outputMultimodalNetwork, String outputSchedule, String outputVehicles) {
		log.info("Writing multimodal Network to " + outputMultimodalNetwork + "...");
		new NetworkWriter(network).write(outputMultimodalNetwork);
		log.info("Writing multimodal Schedule to " + outputSchedule + "...");
		new TransitScheduleWriter(schedule).writeFile(outputSchedule);
		log.info("Writing vehicles to " + outputVehicles + "...");
		new VehicleWriterV1(vehicles).writeFile(outputVehicles);
		log.info("Writing of Network and Schedule done.");
	}
}
