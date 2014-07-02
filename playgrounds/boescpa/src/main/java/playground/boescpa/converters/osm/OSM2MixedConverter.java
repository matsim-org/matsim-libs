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
import playground.boescpa.converters.osm.procedures.*;

/**
 * Provides the main body for the OSM to multimodal network conversion.
 *
 * @author boescpa
 */
public class OSM2MixedConverter {

	private static Logger log = Logger.getLogger(OSM2MixedConverter.class);
	// TODO-boescpa write loggings...
	// log.info("Reading network xml file...");

	private final Network network;
	private final TransitSchedule schedule;
	private final String osmFile;
	private final String hafasFile;
	// TODO-boescpa implement observer for osmFile and hafasFile so that it hasn't to be read x-times...

	private final MultimodalNetworkCreator multimodalNetworkCreator;
	private final PTStationCreator ptStationCreator;
	private final PTLinesCreator ptLinesCreator;

	public OSM2MixedConverter(Network network, TransitSchedule schedule, String osmFile, String hafasFile) {
		this.network = network;
		this.schedule = schedule;
		this.osmFile = osmFile;
		this.hafasFile = hafasFile;

		this.multimodalNetworkCreator = new MultimodalNetworkCreatorDefault(network);
		this.ptStationCreator = new PTStationCreatorDefault(schedule);
		this.ptLinesCreator = new PTLinesCreatorDefault(schedule);
	}

	/**
	 * Converts a given OSM network to a multimodal MATSim network with the help of a HAFAS schedule.
	 */
	public void convertOSM2MultimodalNetwork() {
		/*
		 * Create the standard car network based on established OSM converter.
		 * Create pt-network from OSM for pt-means which use street network.
		 * Merge the two networks.
		 */
		multimodalNetworkCreator.createMultimodalNetwork(osmFile);

		/*
		 * Create pt-Stations from OSM network.
		 * Check and complement pt stations with HAFAS-knowledge.
		 * Link pt stations to the network.
		 *
		 * Assumes that
		 * 	this.createMultimodalNetwork()
		 * has successfully been run before.
		 */
		ptStationCreator.createPTStations(osmFile, hafasFile, network);

		/*
		 * Create all pt-lines of all types using the street network and using the created pt-stations.
		 * Create routes for the pt-lines.
		 * Write schedule from HAFAS-knowledge.
		 * <p/>
		 * Assumes that
		 * this.createMultimodalNetwork() and
		 * this.createPTStations()
		 * have successfully run before this method was called.
		 */
		ptLinesCreator.createPTLines(hafasFile, network);
	}


	/**
	 * Writes the network and the schedule to the specified files.
	 *
	 * @param outputMultimodalNetwork	A string specifying the target network file.
	 * @param outputSchedule			A string specifying the target schedule file.
	 */
	public void writeOutput(String outputMultimodalNetwork, String outputSchedule) {
		new NetworkWriter(network).write(outputMultimodalNetwork);
		new TransitScheduleWriter(schedule).writeFile(outputSchedule);
	}
}
