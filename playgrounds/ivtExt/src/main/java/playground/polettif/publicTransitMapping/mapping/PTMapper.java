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

package playground.polettif.publicTransitMapping.mapping;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

/**
 * Provides the contract for an multithread
 * implementation of public transit mapping.
 *
 * Currently redirects to the only implementation
 * {@link PTMapperImpl}.
 *
 * @author polettif
 */
public abstract class PTMapper {

	protected static Logger log = Logger.getLogger(PTMapper.class);

	protected final PublicTransitMappingConfigGroup config;
	protected Network network;
	protected TransitSchedule schedule;

	/**
	 * Routes the unmapped MATSim Transit Schedule to the network using the file
	 * paths specified in the config. Writes the resulting schedule and network to xml files.<p/>
	 *
	 * @see playground.polettif.publicTransitMapping.workbench.CreateDefaultConfig
	 *
	 * @param args <br/>[0] PublicTransitMapping config file<br/>
	 */
	public static void main(String[] args) {
		if(args.length == 1) {
			run(args[0]);
		} else {
			throw new IllegalArgumentException("Incorrect number of arguments: [0] Public Transit Mapping config file");
		}
	}

	/**
	 * Routes the unmapped MATSim Transit Schedule to the network using the file
	 * paths specified in the config. Writes the resulting schedule and network to xml files.<p/>
	 *
	 * @see playground.polettif.publicTransitMapping.workbench.CreateDefaultConfig
	 *
	 * @param configFile the PublicTransitMapping config file
	 */
	public static void run(String configFile) {
		new PTMapperImpl(configFile).run();
	}

	public static void run(PublicTransitMappingConfigGroup ptmConfig, TransitSchedule schedule, Network network) {
		new PTMapperImpl(ptmConfig, schedule, network).run();
	}

	/**
	 * Use this constructor if you just want to use the config for mapping parameters.
	 * The provided schedule is expected to contain the stops sequence and
	 * the stop facilities each transit route. The routes will be newly routed,
	 * any former routes will be overwritten. Changes are done on the schedule
	 * network provided here.
	 * <p/>
	 *
	 * @param config a PublicTransitMapping config that defines all parameters used
	 *               for mapping.
	 * @param schedule which will be newly routed.
	 * @param network schedule is mapped to this network, is modified
	 */
	public PTMapper(PublicTransitMappingConfigGroup config, TransitSchedule schedule, Network network) {
		this.config = config;
		this.schedule = schedule;
		this.network = network;
	}

	/**
	 * Constructor:<p/>
	 *
	 * Loads the PublicTransitMapping config file. If paths to input files
	 * (schedule and network) are provided in the config, mapping can be run
	 * via {@link #run()}
	 *
	 * @param configPath the config file
	 */
	public PTMapper(String configPath) {
		Config configAll = ConfigUtils.loadConfig(configPath, new PublicTransitMappingConfigGroup() ) ;
		this.config = ConfigUtils.addOrGetModule(configAll, PublicTransitMappingConfigGroup.GROUP_NAME, PublicTransitMappingConfigGroup.class );
		this.schedule = config.getScheduleFile() == null ? null : ScheduleTools.readTransitSchedule(config.getScheduleFile());
		this.network = config.getNetworkFile() == null ? null : NetworkTools.readNetwork(config.getNetworkFile());
	}

	/**
	 * Based on the stop facilities and transit routes in this.schedule
	 * the schedule will be mapped to the given network. Both schedule and
	 * network are modified.<p/>
	 *
	 * Reads the schedule and network file specified in the PublicTransitMapping
	 * config and maps the schedule to the network. Writes the output files as
	 * well if defined in config. The mapping parameters defined in the config
	 * are used.
	 */
	public abstract void run();


	// Setters and Getters
	public void setSchedule(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	public TransitSchedule getSchedule() {
		return schedule;
	}

	public Network getNetwork() {
		return network;
	}
}
