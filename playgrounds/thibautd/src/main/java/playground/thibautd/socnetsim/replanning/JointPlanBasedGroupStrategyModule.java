/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanBasedGroupStrategyModule.java
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * Delegates to a {@link PlanStrategyModule} which handles JointPlans.
 * @author thibautd
 */
public class JointPlanBasedGroupStrategyModule implements GroupStrategyModule {
	private static final Logger log =
		Logger.getLogger(JointPlanBasedGroupStrategyModule.class);

	private final boolean wrapIndividualPlansAndActOnThem;
	private final PlanStrategyModule delegate;

	public JointPlanBasedGroupStrategyModule(
			final PlanStrategyModule module) {
		this( true , module );
	}

	public JointPlanBasedGroupStrategyModule(
			final boolean wrapIndividualPlansAndActOnThem,
			final PlanStrategyModule module) {
		this.wrapIndividualPlansAndActOnThem = wrapIndividualPlansAndActOnThem;
		this.delegate = module;
	}

	@Override
	public void handlePlans(
			final Collection<GroupPlans> groupPlans) {
		delegate.prepareReplanning();

		log.info( "handling "+groupPlans.size()+" groups" );
		for (GroupPlans plans : groupPlans) {
			handlePlans( plans );
		}

		delegate.finishReplanning();
	}

	private void handlePlans( final GroupPlans plans ) {
		for (JointPlan jp : plans.getJointPlans()) {
			delegate.handlePlan( jp );
		}

		if (!wrapIndividualPlansAndActOnThem) return;

		for (Plan p : plans.getIndividualPlans()) {
			Map<Id, Plan> fakeJointPlanMap = new HashMap<Id, Plan>();
			fakeJointPlanMap.put( p.getPerson().getId() , p );
			JointPlan jp = JointPlanFactory.createJointPlan( fakeJointPlanMap , false );
			delegate.handlePlan( jp );
			JointPlanFactory.getPlanLinks().removeJointPlan( jp );
		}
	}
}
