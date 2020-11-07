/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.zone.skims;

import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.core.config.groups.GlobalConfigGroup;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpGlobalTravelTimesMatrixProvider implements Provider<DvrpTravelTimeMatrix> {
	private final DvrpTravelTimeMatrixParams params;
	private final int numberOfThreads;

	@Inject
	@Named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)
	private Network network;

	public DvrpGlobalTravelTimesMatrixProvider(GlobalConfigGroup globalConfig, DvrpTravelTimeMatrixParams params) {
		this.params = params;
		this.numberOfThreads = globalConfig.getNumberOfThreads();
	}

	@Override
	public DvrpTravelTimeMatrix get() {
		return new DvrpTravelTimeMatrix(network, params, numberOfThreads);
	}
}
