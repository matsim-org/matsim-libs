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

import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.VehicleType;

public class VrpAgentSourceQSimModule extends AbstractDvrpModeQSimModule {
	public VrpAgentSourceQSimModule(String mode) {
		super(mode);
	}

	@Override
	protected void configureQSim() {
		bindModal(VrpAgentSource.class).toProvider(modalProvider(getter -> {
			return new VrpAgentSource(getter.getModal(VrpAgentLogic.DynActionCreator.class),
					getter.getModal(Fleet.class), getter.getModal(VrpOptimizer.class), getMode(), getter.get(QSim.class),
					getter.getModal(VehicleType.class));
		}));
		
		addModalQSimComponentBinding().to(modalKey(VrpAgentSource.class));
	}
}
