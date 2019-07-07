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

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author michalm
 */
public class DvrpRoutingNetworkProvider implements Provider<Network> {
	public static final String DVRP_ROUTING = "dvrp_routing";

	private final Network network;
	private final DvrpConfigGroup dvrpCfg;

	@Inject
	public DvrpRoutingNetworkProvider(Network network, DvrpConfigGroup dvrpCfg) {
		this.network = network;
		this.dvrpCfg = dvrpCfg;
	}

	@Override
	public Network get() {
		if (dvrpCfg.getNetworkMode() == null) { // no mode filtering
			return network;
		}

		Network dvrpNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(dvrpNetwork, Collections.singleton(dvrpCfg.getNetworkMode()));
		return dvrpNetwork;
	}

	public static AbstractDvrpModeModule createDvrpModeRoutingNetworkModule(String mode,
			boolean useModeFilteredSubnetwork) {
		return new AbstractDvrpModeModule(mode) {
			@Override
			public void install() {
				if (useModeFilteredSubnetwork) {
					bindModal(Network.class).toProvider(ModalProviders.createProvider(getMode(), getter -> {
						Network subnetwork = NetworkUtils.createNetwork();
						new TransportModeNetworkFilter(
								getter.getNamed(Network.class, DvrpRoutingNetworkProvider.DVRP_ROUTING)).
								filter(subnetwork, Collections.singleton(getMode()));
						new NetworkCleaner().run(subnetwork);
						return subnetwork;
					})).asEagerSingleton();
				} else {
					bindModal(Network.class).to(
							Key.get(Network.class, Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING)));
				}
			}
		};
	}
}
