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

package org.matsim.vsp.ev.dvrp;

import java.net.URL;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.vsp.ev.data.ElectricFleet;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * @author michalm
 */
public class EvDvrpFleetProvider implements Provider<Fleet> {
	@Inject
	@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
	private Network network;

	@Inject
	private ElectricFleet evFleet;

	private final URL url;

	public EvDvrpFleetProvider(URL url) {
		this.url = url;
	}

	@Override
	public Fleet get() {
		FleetImpl fleet = new FleetImpl();
		new VehicleReader(network, fleet).parse(url);

		FleetImpl evDvrpFleet = new FleetImpl();
		for (Vehicle v : fleet.getVehicles().values()) {
			evDvrpFleet.addVehicle(EvDvrpVehicle.create(v, evFleet));
		}
		return evDvrpFleet;
	}

	public static AbstractModule createModule(String mode, URL url) {
		return new AbstractModule() {
			@Override
			public void install() {
				bind(Fleet.class).annotatedWith(Names.named(mode))
						.toProvider(new EvDvrpFleetProvider(url))
						.asEagerSingleton();
			}
		};
	}
}
