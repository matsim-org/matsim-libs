/* *********************************************************************** *
 * project: org.matsim.*
 * DumbExtraPlanRemover.java
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
package playground.thibautd.socnetsim.framework.replanning.removers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.ExtraPlanRemover;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;

/**
 * @author thibautd
 */
public class DumbExtraPlanRemover implements ExtraPlanRemover {
	private final GroupLevelPlanSelector selectorForRemoval;
	private final int maxPlanPerAgent;

	public DumbExtraPlanRemover(
			final GroupLevelPlanSelector selectorForRemoval,
			final int maxPlanPerAgent ) {
		this.selectorForRemoval = selectorForRemoval;
		this.maxPlanPerAgent = maxPlanPerAgent;
	}

	@Override
	public boolean removePlansInGroup(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		int c = 0;
		while ( removeOneExtraPlan( jointPlans , group ) ) c++;
		if (c > 1) {
			// in the clique setting, it is impossible.
			// replace by a warning (or nothing) when there is a setting
			// in which this makes sense.
			throw new RuntimeException( c+" removal iterations were performed for group "+group );
		}
		return c > 0;
	}

	private final boolean removeOneExtraPlan(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		if (maxPlanPerAgent <= 0) return false;

		GroupPlans toRemove = null;
		boolean somethingDone = false;
		final List<Person> personsToHandle = new ArrayList<Person>( group.getPersons() );
		while ( !personsToHandle.isEmpty() ) {
			final Person person = personsToHandle.remove( 0 );
			if (person.getPlans().size() <= maxPlanPerAgent) continue;

			somethingDone = true;
			if (toRemove == null) {
				toRemove = selectorForRemoval.selectPlans( jointPlans , group );
				if ( toRemove == null ) {
					throw new RuntimeException(
							"The selector for removal returned no plan "
							+"for group "+group );
				}
			}

			for (Plan plan : toRemove( jointPlans , person , toRemove )) {
				final Person personToHandle = plan.getPerson();

				if ( personToHandle != person ) {
					final boolean removed = personsToHandle.remove( personToHandle );
					if ( !removed ) throw new RuntimeException( "person "+personToHandle+" is not part of the persons to handle when processing group "+group );
				}

				final boolean removed = personToHandle.getPlans().remove( plan );
				if ( !removed ) throw new RuntimeException( "could not remove plan "+plan+" of person "+personToHandle );

				final JointPlan jpToRemove = jointPlans.getJointPlan( plan );
				if ( jpToRemove != null ) jointPlans.removeJointPlan( jpToRemove );
			}
		}

		return somethingDone;
	}

	private static final Collection<Plan> toRemove(
			final JointPlans jointPlans,
			final Person person,
			final GroupPlans toRemove) {
		for (Plan plan : person.getPlans()) {
			JointPlan jp = jointPlans.getJointPlan( plan );

			if (jp != null && toRemove.getJointPlans().contains( jp )) {
				return jp.getIndividualPlans().values();
			}

			if (jp == null && toRemove.getIndividualPlans().contains( plan )) {
				return Collections.singleton( plan );
			}
		}
		throw new IllegalArgumentException();
	}

}

