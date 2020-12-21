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

package org.matsim.contrib.ev.dvrp;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureModule;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * Use this module in addition to EvModule
 *
 * @author michalm
 */
public class EvDvrpIntegrationModule extends AbstractModule {
	@Override
	public void install() {
		bind(Network.class).annotatedWith(Names.named(ChargingInfrastructureModule.CHARGERS))
				.to(Key.get(Network.class, Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)))
				.asEagerSingleton();
	}
}
