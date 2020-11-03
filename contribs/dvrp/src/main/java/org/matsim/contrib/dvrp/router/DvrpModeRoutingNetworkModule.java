/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.router;

import static org.matsim.contrib.dvrp.run.ModalProviders.createProvider;

import java.util.Collections;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpModeRoutingNetworkModule extends AbstractDvrpModeModule {
	private final boolean useModeFilteredSubnetwork;

	@Inject
	private DvrpConfigGroup dvrpConfigGroup;

	@Inject
	private GlobalConfigGroup globalConfigGroup;

	public DvrpModeRoutingNetworkModule(String mode, boolean useModeFilteredSubnetwork) {
		super(mode);
		this.useModeFilteredSubnetwork = useModeFilteredSubnetwork;
	}

	@Override
	public void install() {
		if (useModeFilteredSubnetwork) {
			checkUseModeFilteredSubnetworkAllowed(getConfig(), getMode());
			bindModal(Network.class).toProvider(createProvider(getMode(), getter -> {
				Network subnetwork = NetworkUtils.createNetwork();
				new TransportModeNetworkFilter(
						getter.getNamed(Network.class, DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)).
						filter(subnetwork, Collections.singleton(getMode()));
				new NetworkCleaner().run(subnetwork);
				return subnetwork;
			})).asEagerSingleton();

			dvrpConfigGroup.getTravelTimeMatrixParams().ifPresent(travelTimeMatrixParams ->//
					bindModal(DvrpTravelTimeMatrix.class).toProvider(createProvider(getMode(),
							getter -> new DvrpTravelTimeMatrix(getter.getModal(Network.class), travelTimeMatrixParams,
									globalConfigGroup.getNumberOfThreads()))).asEagerSingleton());

		} else {
			bindModal(Network.class).to(
					Key.get(Network.class, Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)));
			if (dvrpConfigGroup.getTravelTimeMatrixParams().isPresent()) {
				bindModal(DvrpTravelTimeMatrix.class).to(DvrpTravelTimeMatrix.class);
			}
		}
	}

	public static void checkUseModeFilteredSubnetworkAllowed(Config config, String mode) {
		Set<String> dvrpNetworkModes = DvrpConfigGroup.get(config).getNetworkModes();
		Preconditions.checkArgument(dvrpNetworkModes.isEmpty() || dvrpNetworkModes.contains(mode),
				"DvrpConfigGroup.networkModes must either be empty or contain DVRP mode: %s when 'useModeFilteredSubnetwork' is enabled for this mode",
				mode);
	}
}
