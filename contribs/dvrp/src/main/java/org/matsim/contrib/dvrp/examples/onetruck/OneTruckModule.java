/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.examples.onetruck;

import java.net.URL;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OneTruckModule extends AbstractDvrpModeModule {
	private final URL fleetSpecificationUrl;

	public OneTruckModule(URL fleetSpecificationUrl) {
		super(TransportMode.truck);
		this.fleetSpecificationUrl = fleetSpecificationUrl;
	}

	@Override
	public void install() {
		DvrpModes.registerDvrpMode(binder(), getMode());
		install(DvrpRoutingNetworkProvider.createDvrpModeRoutingNetworkModule(getMode(), false));

		bind(VehicleType.class).annotatedWith(Names.named(VrpAgentSourceQSimModule.DVRP_VEHICLE_TYPE))
				.toInstance(createTruckType());
		install(new FleetModule(getMode(), fleetSpecificationUrl));

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				install(new VrpAgentSourceQSimModule(getMode()));

				addModalComponent(OneTruckRequestCreator.class);
				bindModal(VrpOptimizer.class).to(OneTruckOptimizer.class).asEagerSingleton();
				bindModal(VrpAgentLogic.DynActionCreator.class).to(OneTruckActionCreator.class).asEagerSingleton();
			}
		});
	}

	private static VehicleType createTruckType() {
		VehicleType truckType = VehicleUtils.getFactory().createVehicleType(Id.create("truckType", VehicleType.class));
		truckType.setLength(15.);
		VehicleCapacity vehicleCapacity = new VehicleCapacity();
		vehicleCapacity.setSeats(1);
		truckType.setCapacity(vehicleCapacity);
		return truckType;
	}

}
