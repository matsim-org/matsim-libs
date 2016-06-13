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
package org.matsim.population.algorithms;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PlanImpl;
import org.matsim.withinday.controller.ExecutedPlansService;

/**
 * @author nagel
 *
 */
public final class LastExecutedPlanKeeper implements PlanAlgorithm {

	private ExecutedPlansService executedPlans;

	public LastExecutedPlanKeeper(ExecutedPlansService executedPlans) {
		this.executedPlans = executedPlans;
	}

	@Override
	public void run(Plan plan) {
		Plan newPlan = executedPlans.getAgentRecords().get( plan.getPerson().getId() ) ;
		Gbl.assertNotNull( newPlan ) ;
		((PlanImpl) plan).copyFrom(newPlan);
		// yyyy would not be able to solve this by copy constructor!
	}

}
