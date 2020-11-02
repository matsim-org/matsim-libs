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
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimesMatrixModule extends AbstractModule {
	private final DvrpTravelTimeMatrixParams params;
	private final int numberOfThreads;

	public DvrpTravelTimesMatrixModule(GlobalConfigGroup globalConfig, DvrpTravelTimeMatrixParams params) {
		this.params = params;
		this.numberOfThreads = globalConfig.getNumberOfThreads();
	}

	@Override
	public void install() {
		bind(DvrpTravelTimeMatrix.class).toProvider(new Provider<>() {
			@Inject
			@Named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)
			private Network network;

			@Override
			public DvrpTravelTimeMatrix get() {
				return new DvrpTravelTimeMatrix(network, params, numberOfThreads);
			}
		}).asEagerSingleton();
	}
}
