/* *********************************************************************** *
 * project: org.matsim.*
 * RecomposeJointPlanAlgorithm.java
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
package org.matsim.contrib.socnetsim.framework.replanning.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
public class RecomposeJointPlanAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private final PlanLinkIdentifier linkIdentifier;
	private final JointPlanFactory factory;

	public RecomposeJointPlanAlgorithm(
			final JointPlanFactory jointPlanFactory,
			final PlanLinkIdentifier linkIdentifier) {
		this.factory = jointPlanFactory;
		this.linkIdentifier = linkIdentifier;
	}

	@Override
	public void run(final GroupPlans groupPlans) {
		final Map<Id<Person>, Plan> plansMap = getPlansMap( groupPlans );

		groupPlans.clear();
		while (plansMap.size() > 0) {
			final Plan plan = plansMap.remove( plansMap.keySet().iterator().next() );
			final Map<Id<Person>, Plan> jpMap = new HashMap< >();
			jpMap.put( plan.getPerson().getId() , plan );

			findDependentPlans( plan , jpMap , plansMap );

			if ( jpMap.size() > 1 ) {
				groupPlans.addJointPlan(
						factory.createJointPlan( jpMap ) );
			}
			else {
				groupPlans.addIndividualPlan( plan );
			}
		}
	}

	private Map<Id<Person>, Plan> getPlansMap(final GroupPlans groupPlans) {
		final Map<Id<Person>, Plan> map = new HashMap< >();

		for (Plan p : groupPlans.getIndividualPlans()) {
			map.put( p.getPerson().getId() , p );
		}
		for (JointPlan jp : groupPlans.getJointPlans()) {
			map.putAll( jp.getIndividualPlans() );
		}

		return map;
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
			if ( linkIdentifier.areLinked( plan , toLook ) ) {
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

