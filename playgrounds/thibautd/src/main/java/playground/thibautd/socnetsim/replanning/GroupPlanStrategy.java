/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlanStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;

import playground.thibautd.socnetsim.population.JointPlan;

/**
 * Generalizes the PlanStrategy concept to joint replanning.
 * @author thibautd
 */
public class GroupPlanStrategy {
	private final GroupLevelPlanSelector selector;
	private final List<GroupStrategyModule> strategyModules = new ArrayList<GroupStrategyModule>();

	public GroupPlanStrategy(final GroupLevelPlanSelector selector) {
		this.selector = selector;
	}

	public void addStrategyModule(final GroupStrategyModule module) {
		strategyModules.add( module );
	}

	public void run(final Collection<ReplanningGroup> groups) {
		List<GroupPlans> plansToHandle = new ArrayList<GroupPlans>();

		for (ReplanningGroup group : groups) {
			GroupPlans plans = selector.selectPlans( group );

			if (plansToHandle.size() > 0) {
				plans = GroupPlans.copyPlans( plans );
				plansToHandle.add( plans );
			}

			select( plans );
		}

		for (GroupStrategyModule module : strategyModules) {
			module.handlePlans( plansToHandle );
		}
	}

	private static void select(final GroupPlans plans) {
		for (JointPlan jp : plans.getJointPlans()) {
			for (Plan p : jp.getIndividualPlans().values()) {
				((PersonImpl) p.getPerson()).setSelectedPlan( p );
			}
		}

		for (Plan p : plans.getIndividualPlans()) {
			((PersonImpl) p.getPerson()).setSelectedPlan( p );
		}
	}
}

