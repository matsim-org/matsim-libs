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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Provides the contract for an implementation of a pt-lines creation.
 *
 * @author boescpa
 */
public abstract class PTLinesCreator {

	protected static Logger log = Logger.getLogger(PTLinesCreator.class);

	protected final TransitSchedule schedule;

	protected PTLinesCreator(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public void createPTLines(String hafasFile, Network network) {
		log.info("Creating PT lines...");
		createPTLines(hafasFile);
		createPTRoutes(network);
		writeScheduleForPTLines(hafasFile);
		log.info("Creating PT lines... done.");
	}

	/**
	 * Create all pt-lines of all types of public transport using the street network
	 * and using the created pt-stations. Creation is based on HAFAS-knowledge.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param hafasFile
	 */
	protected abstract void createPTLines(String hafasFile);

	/**
	 * By applying a routing algorithm (e.g. shortest path or OSM-extraction) route from station to
	 * station for each pt-line.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param network
	 */
	protected abstract void createPTRoutes(Network network);

	/**
	 * Based on the schedules in the hafasFile, write the full day schedules for each pt-line.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param hafasFile
	 */
	protected abstract void writeScheduleForPTLines(String hafasFile);


}
