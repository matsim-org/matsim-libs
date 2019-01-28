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

package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class VrpAgentSourceQSimModule extends AbstractDvrpModeQSimModule {
	public static final String DVRP_VEHICLE_TYPE = "dvrp_vehicle_type";

	public VrpAgentSourceQSimModule(String mode) {
		super(mode);
	}

	@Override
	protected void configureQSim() {
		addModalComponent(VrpAgentSource.class, new ModalProviders.AbstractProvider<VrpAgentSource>(getMode()) {
			@Inject
			private QSim qSim;

			@Inject
			@Named(DVRP_VEHICLE_TYPE)
			private VehicleType vehicleType;

			@Override
			public VrpAgentSource get() {
				return new VrpAgentSource(getModalInstance(VrpAgentLogic.DynActionCreator.class),
						getModalInstance(Fleet.class), getModalInstance(VrpOptimizer.class), qSim, vehicleType);
			}
		});
	}
}
