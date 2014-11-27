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

package playground.boescpa.converters.osm.scheduleCreator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * The default implementation of PTStationCreator (using the Swiss-HAFAS-Schedule).
 *
 * @author boescpa
 */
public class PTScheduleCreatorDefault extends PTScheduleCreator {

	public PTScheduleCreatorDefault(TransitSchedule schedule) {
		super(schedule);
	}

	@Override
	public final void createSchedule(String osmFile, String hafasFile, Network network) {
		log.info("Creating the schedule...");
		createPTLines(hafasFile);
		complementPTStations(osmFile);
		log.info("Creating the schedule... done.");
	}

	/**
	 * Create all pt-lines (stops, schedule, but no routes) of all types of public transport
	 * using the HAFAS-schedule.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param hafasFile
	 */
	private void createPTLines(String hafasFile) {
		log.info("Creating pt lines from HAFAS file...");

		// TODO-boescpa Implement createPTLines...
		// work with this.schedule...

		// 1. Create all lines from HAFAS-Schedule
		//		1. Stops
		//		2. Schedule
		// 2. Collect all Stops from created lines
		// 3. Write all Stops into schedule

		log.info("Creating pt lines from HAFAS file... done.");
	}

	/**
	 * Check and correct pt-Station-coordinates with osm-knowledge.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param osmFile
	 */
	private void complementPTStations(String osmFile) {
		log.info("Correcting pt station coordinates based on OSM...");

		// TODO-boescpa Implement complementPTStations...
		// work with this.schedule...

		log.info("Correcting pt station coordinates based on OSM... done.");
	}
}
