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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

/**
 * Provides the contract to create pt lines (stops and scheduled times, no routes) from an OSM network,
 * which are corrected by a given schedule-file.
 * The stops are linked to a given network.
 *
 * @author boescpa
 */
public abstract class PTScheduleCreator {

	protected static Logger log = Logger.getLogger(PTScheduleCreator.class);

	protected final TransitSchedule schedule;
	protected final TransitScheduleFactory scheduleBuilder;

	protected PTScheduleCreator(TransitSchedule schedule) {
		this.schedule = schedule;
		this.scheduleBuilder = this.schedule.getFactory();
	}

	/**
	 * This method creates pt lines (stops and scheduled times, no routes) from the OSM network,
	 * which are corrected by the given schedule-file.
	 * The stops are linked to the given network.
	 *
	 * @param osmFile
	 * @param scheduleFile
	 * @param network
	 */
	public abstract void createSchedule(String osmFile, String scheduleFile, Network network);

}
