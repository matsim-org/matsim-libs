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

package playground.polettif.publicTransitMapping.hafas;

import org.apache.log4j.Logger;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

/**
 * Provides the contract to create pt lines (stops and scheduled times, no routes) from an OSM network,
 * which are corrected by a given schedule-file.
 * The stops are linked to a given network.
 *
 * @author boescpa
 */
public abstract class PTScheduleCreator {

	protected static Logger log = Logger.getLogger(PTScheduleCreator.class);

	private final TransitSchedule schedule;
	private final Vehicles vehicles;

	public PTScheduleCreator(TransitSchedule schedule, Vehicles vehicles) {
		this.schedule = schedule;
		this.vehicles = vehicles;
	}

	/**
	 * This method creates a Transit Schedule which is unlinked to any network, but else complete.
	 *
	 * @param pathToInputFiles
	 */
	public void createSchedule(String pathToInputFiles) {
		getTransitStopCreator().createTransitStops(this.schedule, pathToInputFiles);
		getRouteProfileCreator().createRouteProfiles(this.schedule, pathToInputFiles);
		getDeparturesCreator().createDepartures(this.schedule, this.vehicles, pathToInputFiles);
	}

	public abstract TransitStopCreator getTransitStopCreator();

	public abstract RouteProfileCreator getRouteProfileCreator();

	public abstract DeparturesCreator getDeparturesCreator();

}
