/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package org.matsim.contrib.taxi.benchmark;

import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.*;

/**
 * Behaves like DvrpModule except that VrpTravelTimeEstimator is not bound (install() is overridden)
 * 
 * @author michalm
 *
 */
public class DvrpBenchmarkModule extends DvrpModule {
	@Inject
	private DvrpConfigGroup dvrpCfg;

	@SafeVarargs
	public DvrpBenchmarkModule(Module module, Class<? extends MobsimListener>... listeners) {
		super(module, listeners);
	}

	@Override
	public void install() {
		String mode = dvrpCfg.getMode();
		addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));

		// Visualisation of schedules for DVRP DynAgents
		bind(NonPlanAgentQueryHelper.class).to(VrpAgentQueryHelper.class);

		// Fixed free-speed TT
		install(VrpTravelTimeModules.createFreeSpeedTravelTimeForBenchmarkingModule());
	}
}
