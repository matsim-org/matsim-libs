/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.dvrp.router;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;

/**
 * @author michalm
 */
public class DvrpGlobalRoutingNetworkProvider implements Provider<Network> {
	private static final Logger log = Logger.getLogger(DvrpGlobalRoutingNetworkProvider.class);

	public static final String DVRP_ROUTING = "dvrp_routing";

	private final Network network;
	private final DvrpConfigGroup dvrpCfg;

	@Inject
	public DvrpGlobalRoutingNetworkProvider(Network network, DvrpConfigGroup dvrpCfg) {
		this.network = network;
		this.dvrpCfg = dvrpCfg;
	}

	@Override
	public Network get() {
		//input/output network may not be connected
		logNetworkSize("unfiltered", network);
		if (dvrpCfg.getNetworkModes().isEmpty()) { // no mode filtering
			return network;
		}

		Network filteredNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(filteredNetwork, dvrpCfg.getNetworkModes());
		logNetworkSize("filtered", filteredNetwork);
		return filteredNetwork;
	}

	private void logNetworkSize(String description, Network network) {
		log.info("DVRP global routing network "
				+ description
				+ ": #nodes="
				+ network.getNodes().size()
				+ " #links:"
				+ network.getLinks().size());
	}
}
