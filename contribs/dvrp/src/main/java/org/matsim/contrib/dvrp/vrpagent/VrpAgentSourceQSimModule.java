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
import org.matsim.contrib.dvrp.run.AbstractMultiModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Inject;

public class VrpAgentSourceQSimModule extends AbstractMultiModeQSimModule {

	public VrpAgentSourceQSimModule(String mode) {
		super(mode);
	}

	@Override
	protected void configureQSim() {
		bindModalComponent(VrpAgentSource.class).toProvider(
				new ModalProviders.AbstractProvider<VrpAgentSource>(getMode()) {
					@Inject
					private QSim qSim;

					@Override
					public VrpAgentSource get() {
						return new VrpAgentSource(getModalInstance(VrpAgentLogic.DynActionCreator.class),
								getModalInstance(Fleet.class), getModalInstance(VrpOptimizer.class), qSim);
					}
				}).asEagerSingleton();
	}
}
