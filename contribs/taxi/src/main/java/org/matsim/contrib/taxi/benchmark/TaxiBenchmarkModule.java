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

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.Inject;

/**
 * Behaves like DvrpModule except that VrpTravelTimeEstimator is not bound (install() is overridden)
 * 
 * @author michalm
 *
 */
public class TaxiBenchmarkModule extends TaxiModule {
	@Inject
	private DvrpConfigGroup dvrpCfg;

	/**
	 * Overrides the {@link org.matsim.contrib.dvrp.run.DvrpModule#install() in order to install
	 * freeSpeedTravelTimeForBenchmarkingModule instead of travelTimeEstimatorModule}
	 */
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
