
/* *********************************************************************** *
 * project: org.matsim.*
 * SingleModeNetworksCache.java
 *                                                                         *
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
 * *********************************************************************** */

 package org.matsim.core.router;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;

import com.google.inject.Inject;

public class SingleModeNetworksCache {

	private final Map<String, Network> singleModeNetworksCache = new ConcurrentHashMap<>();
    private final Network fullNetwork;
    private final NetworkConfigGroup networkConfigGroup;

    @Inject
    public SingleModeNetworksCache(Network fullNetwork, NetworkConfigGroup networkConfigGroup) {
        this.fullNetwork = fullNetwork;
        this.networkConfigGroup = networkConfigGroup;
    }

	public Map<String, Network> getSingleModeNetworksCache() {
		return singleModeNetworksCache;
	}

    public Network getOrCreateSingleModeNetwork(final String mode) {
        return getSingleModeNetworksCache().computeIfAbsent(mode, this::filterNetwork);
    }

    private Network filterNetwork(final String mode) {
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(fullNetwork);
        Set<String> modes = new HashSet<>();
        modes.add(mode);

        final Network filteredNetwork = NetworkUtils.createNetwork(networkConfigGroup);
        filter.filter(filteredNetwork, modes);

        return filteredNetwork;
    }
}
