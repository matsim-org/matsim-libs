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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import playground.boescpa.converters.osm.networkCreator.MultimodalNetworkCreator;
import playground.boescpa.converters.osm.networkCreator.MultimodalNetworkCreatorDefault;
import playground.boescpa.converters.osm.scheduleCreator.*;
import playground.boescpa.converters.osm.ptRouter.PTLineRouter;
import playground.boescpa.converters.osm.ptRouter.PTLineRouterDefault;

/**
 * Provides the main body for the OSM to multimodal network conversion.
 *
 * @author boescpa
 */
public class OSM2MixedConverter {

	private static Logger log = Logger.getLogger(OSM2MixedConverter.class);

	private final Network network;
	private final TransitSchedule schedule;
	private final String osmFile;
	private final String scheduleFile;
	// TODO-boescpa implement observer for osmFile and scheduleFile so that it hasn't to be read x-times...

	private final MultimodalNetworkCreator multimodalNetworkCreator;
	private final PTScheduleCreator ptScheduleCreator;
	private final PTLineRouter ptLineRouter;

	public OSM2MixedConverter(Network network, TransitSchedule schedule, String osmFile, String scheduleFile) {
		this.network = network;
		this.schedule = schedule;
		this.osmFile = osmFile;
		this.scheduleFile = scheduleFile;

		this.multimodalNetworkCreator = new MultimodalNetworkCreatorDefault(network);
		this.ptScheduleCreator = new PTScheduleCreatorDefault(schedule);
		this.ptLineRouter = new PTLineRouterDefault(schedule);
	}

	/**
	 * Converts a given OSM network to a multimodal MATSim network with the help of a HAFAS schedule.
	 */
	public void convertOSM2MultimodalNetwork() {
		log.info("Conversion from OSM to multimodal MATSim network...");
		multimodalNetworkCreator.createMultimodalNetwork(osmFile);
		ptScheduleCreator.createSchedule(osmFile, scheduleFile, network);
		ptLineRouter.routePTLines(network);
		log.info("Conversion from OSM to multimodal MATSim network... done.");
	}

	/**
	 * Writes the network and the schedule to the specified files.
	 *
	 * @param outputMultimodalNetwork	A string specifying the target network file.
	 * @param outputSchedule			A string specifying the target schedule file.
	 */
	public void writeOutput(String outputMultimodalNetwork, String outputSchedule) {
		log.info("Writing multimodal Network to " + outputMultimodalNetwork + "...");
		new NetworkWriter(network).write(outputMultimodalNetwork);
		log.info("Writing multimodal Schedule to " + outputSchedule + "...");
		new TransitScheduleWriter(schedule).writeFile(outputSchedule);
		log.info("Writing of Network and Schedule done.");
	}
}
