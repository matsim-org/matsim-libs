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
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.Fleets;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.ev.fleet.ElectricFleet;

import com.google.inject.Inject;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EvDvrpFleetQSimModule extends AbstractDvrpModeQSimModule {
	public EvDvrpFleetQSimModule(String mode) {
		super(mode);
	}

	@Override
	public void configureQSim() {
		bindModal(Fleet.class).toProvider(new ModalProviders.AbstractProvider<Fleet>(getMode()) {
			@Inject
			private ElectricFleet evFleet;

			@Override
			public Fleet get() {
				FleetSpecification fleetSpecification = getModalInstance(FleetSpecification.class);
				Network network = getModalInstance(Network.class);
				return Fleets.createCustomFleet(fleetSpecification,
						s -> EvDvrpVehicle.create(new DvrpVehicleImpl(s, network.getLinks().get(s.getStartLinkId())),
								evFleet));

			}
		}).asEagerSingleton();
	}
}
