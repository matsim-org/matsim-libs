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

package playground.polettif.multiModalMap.mapping;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;

/**
 * Provides the contract for an implementation of ptLines routing.
 *
 * @author polettif
 */
public abstract class PTMapper {

	protected static Logger log = Logger.getLogger(PTMapper.class);

	protected final TransitSchedule schedule;
	protected final PublicTransportMapConfigGroup config;
	protected NetworkFactory networkFactory;

	/**
	 * The provided schedule is expected to already contain for each transit route
	 * 	- the stops in the sequence they will be served.
	 * 	- the scheduled times.
	 *
	 * The routes will be newly routed. Any former routes will be overwritten.
	 * Changes are done on the schedule provided here.
	 *
	 * @param schedule which will be newly routed.
	 */
	protected PTMapper(TransitSchedule schedule, PublicTransportMapConfigGroup config) {
		this.schedule = schedule;
		this.config = config;
	}

	protected PTMapper(TransitSchedule schedule) {
		this.schedule = schedule;
		this.config = new PublicTransportMapConfigGroup();
	}

	public PTMapper(String configPath) {
		Config configAll = ConfigUtils.loadConfig(configPath, new PublicTransportMapConfigGroup() ) ;
		this.config = ConfigUtils.addOrGetModule(configAll, PublicTransportMapConfigGroup.GROUP_NAME, PublicTransportMapConfigGroup.class ) ;

		this.schedule = null;
	}

	/**
	 * Reads the schedule and network file specified in the config and
	 * maps the schedule to the network. Writes the output files as well
	 * if defined in config.
	 */
	public abstract void mapFilesFromConfig();

	/**
	 * Based on the stop facilities and transit routes in this.schedule
	 * the schedule will be mapped to the given network. Both schedule and
	 * network are modified.
	 *
	 * @param network is a multimodal network
	 */
	public abstract void mapScheduleToNetwork(Network network);

}
