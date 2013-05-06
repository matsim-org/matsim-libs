/* *********************************************************************** *
 * project: org.matsim.*
 * IndividualBasedGroupLevelPlanSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning.selectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * Uses an individual-level selector on each agent in a group.
 * The resulting GroupPlans will contain only individual plans,
 * and will <u><b>NOT</b></u> respect joint plan or incompatibility
 * constraints!
 *
 * Plans should be copied and recomposed after the selection.
 *
 * @author thibautd
 */
public class IndividualBasedGroupLevelPlanSelector implements GroupLevelPlanSelector {
	private final PlanSelector delegate;

	public IndividualBasedGroupLevelPlanSelector(
			final PlanSelector individualLevelSelector) {
		this.delegate = individualLevelSelector;
	}

	@Override
	public GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final GroupPlans plans = new GroupPlans();

		for ( Person person : group.getPersons() ) {
			plans.addIndividualPlan( delegate.selectPlan( person ) );
		}

		return plans;
	}
}

