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

package playground.boescpa.converters.osm.procedures;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * The default implementation of PTStationCreator.
 *
 * @author boescpa
 */
public class PTScheduleCreatorDefault extends PTScheduleCreator {

	public PTScheduleCreatorDefault(TransitSchedule schedule) {
		super(schedule);
	}

	@Override
	public final void createSchedule(String osmFile, String hafasFile, Network network) {
		log.info("Creating PT stations...");
		createPTStations(osmFile);
		complementPTStations(hafasFile);
		linkStationsToNetwork(network);
		createPTLines(hafasFile);
		writeScheduleForPTLines(hafasFile);
		log.info("Creating PT stations... done.");
	}

	/**
	 * Create pt stops from the given osmFile based on experimental OSM converters.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param osmFile
	 */
	private void createPTStations(String osmFile) {
		log.info("Creating pt stations from osmFile...");

		// TODO-boescpa Implement createSchedule...
		// write stations into this.schedule...

		// Create pt stops from the given osmFile based on experimental OSM converters.

		log.info("Creating pt stations from osmFile... done.");
	}

	/**
	 * Check and complement pt-Stations and lines with HAFAS-knowledge (hafasFile).
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param hafasFile
	 */
	private void complementPTStations(String hafasFile) {
		log.info("Complementing pt stations based on HAFAS file...");

		// TODO-boescpa Implement complementPTStations...
		// work with this.schedule...

		// Check and complement pt-Stations and lines with HAFAS-knowledge (hafasFile).

		log.info("Complementing pt stations based on HAFAS file... done.");
	}

	/**
	 * Link the pt-stations in the schedule to the closest network links.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param network
	 */
	private void linkStationsToNetwork(Network network) {
		log.info("Linking pt stations to network...");

		// TODO-boescpa Implement linkStationsToNetwork...
		// get pt stations from this.schedule...

		// Link the pt-stations in the schedule to the closest network links.

		log.info("Linking pt stations to network... done.");
	}

	/**
	 * Create all pt-lines of all types of public transport using the street network
	 * and using the created pt-stations. Creation is based on HAFAS-knowledge.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param hafasFile
	 */
	private void createPTLines(String hafasFile) {
		log.info("Creating pt lines from HAFAS file...");

		// TODO-boescpa Implement routePTLines...
		// work with this.schedule...

		// Create all pt-lines of all types of public transport using the street network
		// and using the created pt-stations. Creation is based on HAFAS-knowledge.

		log.info("Creating pt lines from HAFAS file... done.");
	}

	/**
	 * Based on the schedules in the hafasFile, write the full day schedules for each pt-line.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param hafasFile
	 */
	private void writeScheduleForPTLines(String hafasFile) {
		log.info("Writing schedule for pt lines based on HAFAS file...");

		// TODO-boescpa Implement writeScheduleForPTLines...
		// work with this.schedule...

		// Based on the schedules in the hafasFile, write the full day schedules for each pt-line.

		log.info("Writing schedule for pt lines based on HAFAS file... done.");
	}
}
