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

package org.matsim.contrib.ev.dvrp;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetImpl;
import org.matsim.contrib.dvrp.fleet.FleetReader;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EvDvrpFleetModule extends AbstractDvrpModeModule {
	private final String file;

	public EvDvrpFleetModule(String mode, String file) {
		super(mode);
		this.file = file;
	}

	@Override
	public void install() {
		bindModal(FleetSpecification.class).toProvider(() -> {
			FleetSpecificationImpl fleetSpecification = new FleetSpecificationImpl();
			new FleetReader(fleetSpecification).parse(ConfigGroup.getInputFileURL(getConfig().getContext(), file));
			return fleetSpecification;
		}).asEagerSingleton();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(Fleet.class).toProvider(new ModalProviders.AbstractProvider<Fleet>(getMode()) {
					@Inject
					private Config config;

					@Inject
					private ElectricFleet evFleet;

					@Inject
					@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
					private Network network;

					@Override
					public Fleet get() {
						FleetSpecification fleetSpecification = getModalInstance(FleetSpecification.class);

						FleetImpl evDvrpFleet = new FleetImpl();
						for (DvrpVehicleSpecification s : fleetSpecification.getVehicleSpecifications().values()) {
							evDvrpFleet.addVehicle(EvDvrpVehicle.create(
									DvrpVehicleImpl.createWithLinkProvider(s, network.getLinks()::get), evFleet));
						}
						return evDvrpFleet;
					}
				}).asEagerSingleton();
			}
		});
	}
}
