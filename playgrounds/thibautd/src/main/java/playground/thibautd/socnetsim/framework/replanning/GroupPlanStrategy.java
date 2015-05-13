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
package playground.thibautd.socnetsim.framework.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;

/**
 * Generalizes the PlanStrategy concept to joint replanning.
 * @author thibautd
 */
public class GroupPlanStrategy {
	private final GroupLevelPlanSelector selector;
	private final List<GenericStrategyModule<GroupPlans>> strategyModules =
			new ArrayList<GenericStrategyModule<GroupPlans>>();

	public GroupPlanStrategy(final GroupLevelPlanSelector selector) {
		this.selector = selector;
	}

	public void addStrategyModule(final GenericStrategyModule<GroupPlans> module) {
		strategyModules.add( module );
	}

	public void run(
			// XXX what is the best at this level: ReplanningContext or ControllerRegistry?
			final ReplanningContext replanningContext,
			final JointPlans jointPlans,
			final Collection<ReplanningGroup> groups) {
		List<GroupPlans> plansToHandle = new ArrayList<GroupPlans>();

		Counter selectCounter = new Counter( "["+selector.getClass().getSimpleName()+"] selecting plan # " );
		for (ReplanningGroup group : groups) {
			selectCounter.incCounter();
			GroupPlans plans = selector.selectPlans(
					jointPlans,
					group );

			if (plans == null) {
				// this is a valid output from the selector,
				// if no plans combination is possible.
				selectCounter.printCounter();
				throw new RuntimeException( "no plan returned by the selector for group "+group );
			}

			if (strategyModules.size() > 0) {
				plans = GroupPlans.copyPlans( jointPlans.getFactory() , plans );
				plansToHandle.add( plans );
				assert !jointPlansAreRegistered( plans , jointPlans );
			}

			select( plans );
		}
		selectCounter.printCounter();

		for (GenericStrategyModule<GroupPlans> module : strategyModules) {
			module.handlePlans( replanningContext , plansToHandle );
		}

		// the modules are allowed to modify joint structure:
		// register joint plans afterhand only.
		for (GroupPlans groupPlans : plansToHandle) {
			jointPlans.addJointPlans( groupPlans.getJointPlans() );
		}
	}

	private static boolean jointPlansAreRegistered(
			final GroupPlans plans,
			final JointPlans jointPlans) {
		for (JointPlan jp : plans.getJointPlans()) {
			if (jointPlans.contains( jp )) return true;
		}
		return false;
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

	@Override
	public String toString() {
		return "["+getClass().getSimpleName()+": "
			+selector.getClass().getSimpleName()
			+";"+strategyModules+"]";
	}
}

