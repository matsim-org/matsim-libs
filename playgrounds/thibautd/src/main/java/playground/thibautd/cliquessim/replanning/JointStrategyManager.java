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
package playground.thibautd.cliquessim.replanning;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.cliquessim.population.Clique;
import playground.thibautd.cliquessim.population.Cliques;
import playground.thibautd.cliquessim.replanning.selectors.WorstJointPlanForRemovalSelector;

/**
 * Custom {@link StrategyManager} allowing to select and optimize joint plans
 * rather than individual ones.
 * Implements the hooks provided by StrategyManager.
 *
 * @author thibautd
 */
public class JointStrategyManager extends StrategyManager {
	private static final Logger log =
		Logger.getLogger(JointStrategyManager.class);


	// TODO: pass it in a less hard-coded way. Beware on consistency with
	// StrategyManager if doing this!
	private final PlanSelector removalPlanSelector = 
		new WorstJointPlanForRemovalSelector();

	// This is quite ugly. The reason for it is that there are TWO replanning listenners,
	// the core one and the joint plan one. The joint passes a population of clique,
	// which is what we want, but we can't remove the core listenner, and overriding
	// the loadCoreListenners method breaks too often. The trick is thus to check
	// what is the passed population, and to return an "inactive" strategy if it is
	// not a population of cliques.
	private boolean isAPopulationOfCliques = false;
	private PlanStrategy inactiveStrategy = new PlanStrategyImpl( new KeepSelected() );

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
	protected void beforePopulationRunHook(final Population population) {
		if (population instanceof Cliques) {
			Cliques cliques = (Cliques) population;
			int maxNumPlans = super.getMaxPlansPerAgent();

			if (maxNumPlans >0) {
				for (Clique clique : cliques.getCliques().values()) {
					//log.debug("clique has "+clique.getPlans().size()+" plans");
					if (clique.getPlans().size() > maxNumPlans) {
						removePlans(clique, maxNumPlans);
					}
				}
			}

			log.debug( "Got a population of cliques. Replanning will be performed." );
			isAPopulationOfCliques = true;
		}
		else {
			log.debug( "Did not got a population of cliques. No replanning will be performed." );
			isAPopulationOfCliques = false;
		}
	}

	private final void removePlans(
			final Clique clique,
			final int maxNumPlans) {
		while (clique.getPlans().size() > maxNumPlans) {
			Plan plan = this.removalPlanSelector.selectPlan(clique);
			clique.removePlan(plan);
		}
	}

	@Override
	public PlanStrategy chooseStrategy(final Person person) {
		if (isAPopulationOfCliques) return super.chooseStrategy( person );
		return inactiveStrategy;
	}
}
