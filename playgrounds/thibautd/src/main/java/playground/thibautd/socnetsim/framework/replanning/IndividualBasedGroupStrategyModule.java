/* *********************************************************************** *
 * project: org.matsim.*
 * IndividualBasedPlanStrategyModule.java
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

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * Delegates action on individual plans to a {@link PlanStrategyModule}
 * @author thibautd
 */
public class IndividualBasedGroupStrategyModule implements GenericStrategyModule<GroupPlans> {
	private static final Logger log =
		Logger.getLogger(IndividualBasedGroupStrategyModule.class);

	private final boolean actOnPlansInJointPlans;
	private final PlanStrategyModule delegate;

	public IndividualBasedGroupStrategyModule(
			final PlanStrategyModule module) {
		this( true , module );
	}

	public IndividualBasedGroupStrategyModule(
			final boolean actOnPlansInJointPlans,
			final PlanStrategyModule module) {
		this.actOnPlansInJointPlans = actOnPlansInJointPlans;
		this.delegate = module;
	}

	@Override
	public void handlePlans(
			final ReplanningContext replanningContext,
			final Collection<GroupPlans> groupPlans) {
		delegate.prepareReplanning( replanningContext );

		log.info( "handling "+groupPlans.size()+" groups" );
		for (GroupPlans plans : groupPlans) {
			handlePlans( plans );
		}

		delegate.finishReplanning();
	}

	private void handlePlans( final GroupPlans plans ) {
		for (Plan p : plans.getIndividualPlans()) {
			delegate.handlePlan( p );
		}

		if (!actOnPlansInJointPlans) return;

		for (JointPlan jp : plans.getJointPlans()) {
			for (Plan p : jp.getIndividualPlans().values()) {
				delegate.handlePlan( p );
			}
		}
	}

	private String name = null;
	@Override
	public String toString() {
		if ( name == null ) {
			name = name();
		}
		return name;
	}

	public String name() {
		String delegateName = delegate.getClass().getSimpleName();
		if (delegateName.length() == 0 && delegate instanceof AbstractMultithreadedModule) {
			// anonymous class
			try {
				delegateName = ((AbstractMultithreadedModule) delegate).getPlanAlgoInstance().getClass().getSimpleName();
			}
			catch (Exception e) {
				// no name: no big deal.
				// such exceptions can happen if the delegate tries to get the replanning
				// context, which is not yet set when this method is called.
			}
		}

		return "["+getClass().getSimpleName()+": "+delegateName+"]";
	}
}

