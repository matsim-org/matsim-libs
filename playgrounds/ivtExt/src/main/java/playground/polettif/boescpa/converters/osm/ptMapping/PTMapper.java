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

package playground.polettif.boescpa.converters.osm.ptMapping;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

/**
 * Provides the contract for an implementation of pt-lines routing.
 *
 * @author boescpa
 */
public abstract class PTMapper {

	protected static Logger log = Logger.getLogger(PTMapper.class);

	protected final TransitSchedule schedule;
	protected final TransitScheduleFactory scheduleFactory;
	protected Network network;
	protected NetworkFactory networkFactory;

	/**
	 * The provided schedule is expected to already contain for each line
	 * 	- the stops in the sequence they will be served.
	 * 	- the scheduled times.
	 * The routes will be newly routed. Any former routes will be overwritten.
	 * Changes are done on the schedule provided here.
	 *
	 * @param schedule which will be newly routed.
	 */
	protected PTMapper(TransitSchedule schedule) {
		this.schedule = schedule;
		this.scheduleFactory = this.schedule.getFactory();
	}

	protected PTMapper(TransitSchedule schedule, Network network) {
		this(schedule);
		this.setNetwork(network);
	}

	/**
	 * Based on the stops in this.schedule und given the provided network, the lines will be routed.
	 *
	 * @param network is a multimodal network (see MultimodalNetworkCreator)
	 */
	public abstract void routePTLines(Network network);

	protected void setNetwork(Network network) {
		this.network = network;
		this.networkFactory = network.getFactory();
	}
}
