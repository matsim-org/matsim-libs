/* *********************************************************************** *
 * project: org.matsim.*
 * WeakSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;

/**
 * create a "weak" selector (only considering strong ties) from a "normal"
 * selector (considering joint plans).
 * <br>
 * This works as expected only if the joint plans were created using the "strong"
 * version of the plan link identifier used here.
 * @author thibautd
 */
public class WeakSelector implements GroupLevelPlanSelector {
	private final GroupLevelPlanSelector delegate;
	private final PlanLinkIdentifier weakPlanLinkIdentifier;

	public WeakSelector(
			final PlanLinkIdentifier weakPlanLinkIdentifier,
			final GroupLevelPlanSelector delegate ) {
		this.weakPlanLinkIdentifier = weakPlanLinkIdentifier;
		this.delegate = delegate;
	}

	/**
	 * Note that the joint plans in the group plans will not be joint plans
	 * referenced in the container passed as parameter!
	 */
	@Override
	public GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final Collection<JointPlan> jpCollection = getJointPlans( group , jointPlans );
		final JointPlans jointPlansInstance = new JointPlans();
		fillWithWeakJointPlans( jointPlansInstance , jpCollection );

		final GroupPlans selected = delegate.selectPlans( jointPlansInstance , group );

		// this is necessary in order to avoid ridiculous memory consumption, with
		// joint plan caches being full of unused joint plans.
		// As we clean it, we could as well re-use it, to avoid having thousands of
		// loose objects lying around (not done, as the actual effect on performance
		// is probably minimal)
		jointPlansInstance.clear();
		return selected;
	}

	private Collection<JointPlan> getJointPlans(
			final ReplanningGroup group,
			final JointPlans jointPlans) {
		final Set<JointPlan> set = new HashSet<JointPlan>();

		for ( Person person : group.getPersons() ) {
			for ( Plan plan : person.getPlans() ) {
				final JointPlan jp = jointPlans.getJointPlan( plan );
				if ( jp != null ) set.add( jp );
			}
		}
		return set;
	}

	public void fillWithWeakJointPlans(
			final JointPlans weakPlans,
			final Collection<JointPlan> jointPlans) {
		for ( JointPlan fullJointPlan : jointPlans ) {
			final Map<Id<Person>, Plan> plansMap = new HashMap< >( fullJointPlan.getIndividualPlans() );

			while ( ! plansMap.isEmpty() ) {
				final Plan plan = plansMap.remove( plansMap.keySet().iterator().next() );
				final Map<Id<Person>, Plan> jpMap = new HashMap< >();
				jpMap.put( plan.getPerson().getId() , plan );

				findDependentPlans( plan , jpMap , plansMap );

				if ( jpMap.size() > 1 ) {
					weakPlans.addJointPlan(
							weakPlans.getFactory().createJointPlan( jpMap ) );
				}
			}
		}
	}

	// DFS
	private void findDependentPlans(
			final Plan plan,
			final Map<Id<Person>, Plan> dependantPlans,
			final Map<Id<Person>, Plan> plansToLook) {
		final List<Plan> dependentPlansList = new ArrayList<Plan>();

		final Iterator<Plan> toLookIt = plansToLook.values().iterator();
		while ( toLookIt.hasNext() ) {
			final Plan toLook = toLookIt.next();
			if ( weakPlanLinkIdentifier.areLinked( plan , toLook ) ) {
				dependentPlansList.add( toLook );
				toLookIt.remove();
			}
		}

		for (Plan depPlan : dependentPlansList) {
			dependantPlans.put( depPlan.getPerson().getId() , depPlan );
			findDependentPlans(
					depPlan,
					dependantPlans,
					plansToLook);
		}
	}
}

