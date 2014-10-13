/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.pseudosimulation.replanning.modules;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PSimReRoute extends AbstractMultithreadedModule {
	private final Scenario scenario;

	public PSimReRoute(Scenario scenario) {
		super(scenario.getConfig().global());
		this.scenario = scenario;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {

		return new PSimPlanRouter(getReplanningContext().getTripRouter(), scenario.getActivityFacilities());
	}

}
