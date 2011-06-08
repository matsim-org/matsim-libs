/* *********************************************************************** *
 * project: org.matsim.*
 * JointReRouteAlgo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning.reroute;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * Similar to the {@link org.matsim.core.replanning.modules.ReRoute} algorithm, on all plans of a joint plan.
 *
 * Execution of the ReRoute on a JointPlan fails, as a strict act/leg
 * alternance is expected (which is not the case between the individual 
 * plans of a JointPlan.
 *
 * @author thibautd
 */
public class JointReRouteAlgo implements PlanAlgorithm {

	private final PlansCalcRoute routingAlgo;

	public JointReRouteAlgo(final Controler controler) {
		this.routingAlgo = (PlansCalcRoute) controler.createRoutingAlgorithm();
	}	

	@Override
	public void run(final Plan plan) {
		if (plan instanceof JointPlan) {
			run((JointPlan) plan);
		}
		else {
			this.routingAlgo.run(plan);
		}
	}

	private void run(final JointPlan plan) {
		for (Plan indivPlan: plan.getIndividualPlans().values()) {
			this.routingAlgo.run(indivPlan);
		}
	}
}

