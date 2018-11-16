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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OneTruckModule extends AbstractModule {
	private final String trucksFile;

	public OneTruckModule(String truckFile) {
		this.trucksFile = truckFile;
	}

	@Override
	public void install() {
		bind(VehicleType.class).annotatedWith(Names.named(VrpAgentSource.DVRP_VEHICLE_TYPE))
				.toInstance(createTruckType());
		install(FleetProvider.createModule(TransportMode.truck,
				ConfigGroup.getInputFileURL(getConfig().getContext(), trucksFile)));

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(OneTruckRequestCreator.class).asEagerSingleton();
				bind(DvrpModes.key(VrpOptimizer.class, TransportMode.truck)).to(OneTruckOptimizer.class)
						.asEagerSingleton();
				bind(DvrpModes.key(VrpAgentLogic.DynActionCreator.class, TransportMode.truck)).to(
						OneTruckActionCreator.class).asEagerSingleton();
			}
		});
	}

	private static VehicleType createTruckType() {
		VehicleType truckType = VehicleUtils.getFactory().createVehicleType(Id.create("truckType", VehicleType.class));
		truckType.setLength(15.);
		VehicleCapacity vehicleCapacity = new VehicleCapacityImpl();
		vehicleCapacity.setSeats(1);
		truckType.setCapacity(vehicleCapacity);
		return truckType;
	}

}
