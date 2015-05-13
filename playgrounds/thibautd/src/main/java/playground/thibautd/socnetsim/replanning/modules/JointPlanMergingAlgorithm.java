/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanMergingAlgorithm.java
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
package playground.thibautd.socnetsim.replanning.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlanFactory;
import playground.thibautd.socnetsim.framework.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

class JointPlanMergingAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private final double probAcceptance;
	private final Random random;
	private final JointPlanFactory factory;

	public JointPlanMergingAlgorithm(
			final JointPlanFactory factory,
			final double probAcceptance,
			final Random random) {
		this.probAcceptance = probAcceptance;
		this.random = random;
		this.factory = factory;
	}

	@Override
	public void run(final GroupPlans plans) {
		//unregisterJointPlans( plans );
		mergePlans( plans );
	}

	private void mergePlans(final GroupPlans plans) {
		final List<JointPlanBuilder> builders = new ArrayList<JointPlanBuilder>();

		for ( JointPlan jp : plans.getJointPlans() ) {
			Collections.shuffle( builders , random );
			if ( add( builders , jp ) ) continue;

			final JointPlanBuilder builder = new JointPlanBuilder();
			builder.addJointPlan( jp );
			builders.add( builder );
		}

		for ( Plan p : plans.getIndividualPlans() ) {
			Collections.shuffle( builders , random );
			if ( add( builders , p ) ) continue;

			final JointPlanBuilder builder = new JointPlanBuilder();
			builder.addIndividualPlan( p );
			builders.add( builder );
		}

		// update
		plans.clear();

		for (JointPlanBuilder builder : builders) {
			if (builder.isJoint()) {
				plans.addJointPlan( builder.build() );
			}
			else {
				plans.addIndividualPlan( builder.getIndividualPlan() );
			}
		}
	}

	private boolean add( final List<JointPlanBuilder> builders , final JointPlan jp ) {
		for (JointPlanBuilder builder : builders) {
			if (random.nextDouble() > probAcceptance) continue;

			builder.addJointPlan( jp );
			return true;
		}
		return false;
	}
	
	private boolean add( final List<JointPlanBuilder> builders , final Plan p ) {
		for (JointPlanBuilder builder : builders) {
			if (random.nextDouble() > probAcceptance) continue;

			builder.addIndividualPlan( p );
			return true;
		}
		return false;
	}

	//private void unregisterJointPlans(final GroupPlans plans) {
	//	for (JointPlan jp : plans.getJointPlans()) {
	//		JointPlanFactory.getPlanLinks().removeJointPlan( jp );
	//	}
	//}

	private class JointPlanBuilder {
		private final Map<Id<Person>, Plan> plans = new HashMap<Id<Person>, Plan>();

		public void addJointPlan(final JointPlan jp) {
			plans.putAll( jp.getIndividualPlans() );
		}

		public void addIndividualPlan(final Plan plan) {
			plans.put( plan.getPerson().getId() , plan );
		}

		public boolean isJoint() {
			return plans.size() > 1;
		}

		public Plan getIndividualPlan() {
			return plans.values().iterator().next();
		}

		public JointPlan build() {
			return factory.createJointPlan( plans );
		}
	}
}
