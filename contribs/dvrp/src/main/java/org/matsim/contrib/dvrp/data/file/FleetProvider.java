/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.data.file;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class FleetProvider implements Provider<Fleet> {
	@Inject
	@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
	private Network network;

	@Inject
	private Config config;

	private final String file;

	public FleetProvider(String file) {
		this.file = file;
	}

	@Override
	public Fleet get() {
		FleetSpecificationImpl fleetSpecification = new FleetSpecificationImpl();
		new VehicleReader(fleetSpecification).parse(ConfigGroup.getInputFileURL(config.getContext(), file));
		return FleetImpl.create(fleetSpecification, network.getLinks()::get);
	}
}
