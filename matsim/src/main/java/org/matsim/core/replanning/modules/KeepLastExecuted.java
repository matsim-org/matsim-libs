/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.replanning.modules;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.controller.ExecutedPlansService;

/**
 * @author nagel
 *
 */
public class KeepLastExecuted extends AbstractMultithreadedModule {

	private ExecutedPlansService executedPlans;

	public KeepLastExecuted(Config config, ExecutedPlansService executedPlans) {
		super(config.global());
		this.executedPlans = executedPlans;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlanAlgorithm() {
			@Override
			public void run(Plan plan) {
				Plan newPlan = executedPlans.getExecutedPlans().get( plan.getPerson().getId() ) ;
				Gbl.assertNotNull( newPlan ) ;
				PopulationUtils.copyFromTo(newPlan, plan, true);
			}
		};
	}

}
