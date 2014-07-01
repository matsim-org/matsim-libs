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
 * Provides the contract to create pt stations from an OSM network, which are corrected by
 * a given HAFAS-file and linked to a given network.
 *
 * @author boescpa
 */
public abstract class PTStationCreator {

	protected final TransitSchedule schedule;

	public PTStationCreator(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	/**
	 * Create pt stops from the given osmFile based on experimental OSM converters.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param osmFile
	 */
	public abstract void createPTStations(String osmFile);

	/**
	 * Check and complement pt-Stations and lines with HAFAS-knowledge (hafasFile).
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param hafasFile
	 */
	public abstract void complementPTStations(String hafasFile);

	/**
	 * Link the pt-stations in the schedule to the closest network links.
	 *
	 * Writes the resulting schedule into this.schedule.
	 *
	 * @param network
	 */
	public abstract void linkStationsToNetwork(Network network);

}
