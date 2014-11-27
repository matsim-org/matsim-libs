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

package playground.boescpa.converters.osm.ptRouter;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Default implementation of PTLinesCreator.
 *
 * @author boescpa
 */
public class PTLineRouterDefault extends PTLineRouter {

	public PTLineRouterDefault(TransitSchedule schedule) {
		super(schedule);
	}

	@Override
	public void routePTLines(Network network) {
		log.info("Creating PT lines...");
		linkStationsToNetwork(network);
		createPTRoutes(network);
		log.info("Creating PT lines... done.");
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
	 * By applying a routing algorithm (e.g. shortest path or OSM-extraction) route from station to
	 * station for each pt-line.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param network
	 */
	private void createPTRoutes(Network network) {
		log.info("Creating pt routes...");

		// TODO-boescpa Implement createPTRoutes...
		// work with this.schedule...

		// By applying a routing algorithm (e.g. shortest path or OSM-extraction) route from station to
		// station for each pt-line.

		log.info("Creating pt routes... done.");
	}

}
