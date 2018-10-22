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

import javax.inject.Provider;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class VrpAgentSourceQSimModule extends AbstractQSimModule {
	public final static String VRP_AGENT_SOURCE_NAME_PREFIX = "VrpAgentSource_";

	private final String mode;

	public VrpAgentSourceQSimModule(String mode) {
		this.mode = mode;
	}

	@Override
	protected void configureQSim() {
		bindNamedComponent(VrpAgentSource.class, VRP_AGENT_SOURCE_NAME_PREFIX + mode).toProvider(new VrpAgentSourceProvider(mode)).asEagerSingleton();
	}

	public static void configureComponents(QSimComponentsConfig components, String mode) {
		components.addNamedComponent(VrpAgentSourceQSimModule.VRP_AGENT_SOURCE_NAME_PREFIX + mode);
	}

	public static class VrpAgentSourceProvider implements Provider<VrpAgentSource> {
		private final String mode;

		@Inject
		private Injector injector;
		@Inject
		private QSim qSim;

		public VrpAgentSourceProvider(String mode) {
			this.mode = mode;
		}

		@Override
		public VrpAgentSource get() {
			Named modeNamed = Names.named(mode);
			VrpAgentLogic.DynActionCreator actionCreator = injector.getInstance(
					Key.get(VrpAgentLogic.DynActionCreator.class, modeNamed));
			Fleet fleet = injector.getInstance(Key.get(Fleet.class, modeNamed));
			VrpOptimizer optimizer = injector.getInstance(Key.get(VrpOptimizer.class, modeNamed));
			return new VrpAgentSource(actionCreator, fleet, optimizer, qSim);
		}
	}
}
