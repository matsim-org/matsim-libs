/* *********************************************************************** *
 * project: org.matsim.*
 * JointStrategyManager.java
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
package playground.thibautd.jointtripsoptimizer.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.PopulationOfCliques;
import playground.thibautd.jointtripsoptimizer.replanning.selectors.WorstJointPlanForRemovalSelector;

/**
 * Custom StrategyManager allowing to select and optimize joint schedules
 * rather than individual ones.
 * Implements the hooks provided by StrategyManager.
 *
 * @author thibautd
 */
public class JointStrategyManager extends StrategyManager {

	// TODO: pass it in a less hard-coded way. Beware on consistency with
	// StrategyManager if doing this!
	private final PlanSelector removalPlanSelector = 
		new WorstJointPlanForRemovalSelector();
	

	/*
	 * =========================================================================
	 * Hooks provided by the super class:
	 * =========================================================================
	 */
	/**
	 * Reduces the number of JointPlans per clique to the maximum number of plans.
	 * This has to be done here, has the base StrategyManager is only able to
	 * operate selection on PersonImpl agents.
	 */
	@Override
	protected void beforePopulationRunHook(Population population) {
		if (population instanceof PopulationOfCliques) {
			PopulationOfCliques cliques = (PopulationOfCliques) population;
			int maxNumPlans = super.getMaxPlansPerAgent();

			if (maxNumPlans >0) {
				for (Clique clique : cliques.getCliques().values()) {
					//log.debug("clique has "+clique.getPlans().size()+" plans");
					if (clique.getPlans().size() > maxNumPlans) {
						removePlans(clique, maxNumPlans);
					}
				}
			}
		} else {
			throw new IllegalArgumentException("JointStrategyManager has been "+
					"ran on a population of non-clique agents.");
		}
	}

	private final void removePlans(Clique clique, int maxNumPlans) {
		while (clique.getPlans().size() > maxNumPlans) {
			Plan plan = this.removalPlanSelector.selectPlan(clique);
			clique.removePlan(plan);
		}
	}
}
