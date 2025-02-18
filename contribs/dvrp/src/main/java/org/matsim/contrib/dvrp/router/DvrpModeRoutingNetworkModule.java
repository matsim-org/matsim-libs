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

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.contrib.zone.skims.FreeSpeedTravelTimeMatrix;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpModeRoutingNetworkModule extends AbstractDvrpModeModule {
	private final boolean useModeFilteredSubnetwork;
	private final String modalCachePath;

	@Inject
	private DvrpConfigGroup dvrpConfigGroup;

	@Inject
	private GlobalConfigGroup globalConfigGroup;

	@Inject
	private QSimConfigGroup qSimConfigGroup;

	public DvrpModeRoutingNetworkModule(String mode, boolean useModeFilteredSubnetwork, String modalCachePath) {
		super(mode);
		this.useModeFilteredSubnetwork = useModeFilteredSubnetwork;
		this.modalCachePath = modalCachePath;
	}

	public DvrpModeRoutingNetworkModule(String mode, boolean useModeFilteredSubnetwork) {
		this(mode, useModeFilteredSubnetwork, null);
	}

	@Override
	public void install() {
		if (useModeFilteredSubnetwork) {
			//filter out the subnetwork
			checkUseModeFilteredSubnetworkAllowed(getConfig(), getMode());
			bindModal(Network.class).toProvider(modalProvider(getter -> {
				Network subnetwork = NetworkUtils.createNetwork(getConfig().network());
				new TransportModeNetworkFilter(
						getter.getNamed(Network.class, DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)).filter(
						subnetwork, Collections.singleton(getMode()));
				new NetworkCleaner().run(subnetwork);
				return subnetwork;
			})).asEagerSingleton();

			//use mode-specific travel time matrix built for this subnetwork
			//lazily initialised: optimisers may not need it
			bindModal(TravelTimeMatrix.class).toProvider(modalProvider(
					getter -> {
						Network network = getter.getModal(Network.class);
						DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
						ZoneSystem zoneSystem = ZoneSystemUtils.createZoneSystem(getConfig().getContext(), network,
							matrixParams.getZoneSystemParams(), getConfig().global().getCoordinateSystem(), zone -> true);
						
						
						if (modalCachePath == null) {
							return FreeSpeedTravelTimeMatrix.createFreeSpeedMatrix(network, zoneSystem, matrixParams, globalConfigGroup.getNumberOfThreads(),
								qSimConfigGroup.getTimeStepSize());
						} else {
							URL cachePath = ConfigGroup.getInputFileURL(getConfig().getContext(), modalCachePath);
							return FreeSpeedTravelTimeMatrix.createFreeSpeedMatrixFromCache(network, zoneSystem, matrixParams, globalConfigGroup.getNumberOfThreads(),
								qSimConfigGroup.getTimeStepSize(), cachePath);
						}
                    })).in(Singleton.class);
		} else {
			//use DVRP-routing (dvrp-global) network
			bindModal(Network.class).to(
					Key.get(Network.class, Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)));

			//use dvrp-global travel time matrix
			bindModal(TravelTimeMatrix.class).to(TravelTimeMatrix.class);
		}
	}

	public static void checkUseModeFilteredSubnetworkAllowed(Config config, String mode) {
		Set<String> dvrpNetworkModes = DvrpConfigGroup.get(config).networkModes;
		Preconditions.checkArgument(dvrpNetworkModes.isEmpty() || dvrpNetworkModes.contains(mode),
				"DvrpConfigGroup.networkModes must either be empty or contain DVRP mode: %s when 'useModeFilteredSubnetwork' is enabled for this mode",
				mode);
	}
}
