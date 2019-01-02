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

package org.matsim.contrib.dvrp.run;

import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpModeQSimModule extends AbstractQSimModule {
	private final String mode;
	private final boolean installPassengerEngineModule;

	public DvrpModeQSimModule(String mode, boolean installPassengerEngineModule) {
		this.mode = mode;
		this.installPassengerEngineModule = installPassengerEngineModule;
	}

	@Override
	protected void configureQSim() {
		install(new VrpAgentSourceQSimModule(mode));

		if (installPassengerEngineModule) {
			install(new PassengerEngineQSimModule(mode));
		}
	}

	public void configureComponents(QSimComponentsConfig components) {
		components.addComponent(DvrpModes.mode(mode));
	}

	public static class Builder {
		private final String mode;

		private boolean installPassengerEngineModule = true;

		public Builder(String mode) {
			this.mode = mode;
		}

		public Builder setInstallPassengerEngineModule(boolean installPassengerEngineModule) {
			this.installPassengerEngineModule = installPassengerEngineModule;
			return this;
		}

		public DvrpModeQSimModule build() {
			return new DvrpModeQSimModule(mode, installPassengerEngineModule);
		}
	}
}
