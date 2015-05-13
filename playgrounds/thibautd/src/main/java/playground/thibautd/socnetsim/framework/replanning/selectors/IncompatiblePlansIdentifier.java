/* *********************************************************************** *
 * project: org.matsim.*
 * IncompatiblePlansIdentifier.java
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
package playground.thibautd.socnetsim.framework.replanning.selectors;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;

/**
 * Allows to define incomaptibility relations between plans.
 * Two incompatible plans cannot be selected at the same time.
 * This is useful for instance when using vehicle ressources,
 * to enforce that only one joint plan related to one given vehicle
 * is selected at the same time.
 *
 * @author thibautd
 */
public abstract class IncompatiblePlansIdentifier<T> {
	/**
	 * @param plan the plan for which the incompatible plans are to identify
	 * @return a collection containing ids of "incompatibility groups".
	 * Two plans are considered incompatible if they have at least one group
	 * in common.
	 */
	public abstract Set<Id<T>> identifyIncompatibilityGroups(Plan plan);

	public Set<Id<T>> identifyIncompatibilityGroups(final JointPlan jp) {
		final Set<Id<T>> groups = new HashSet<>();
		for ( Plan p : jp.getIndividualPlans().values() ) {
			groups.addAll( identifyIncompatibilityGroups( p ) );
		}
		return groups;
	}

	public Set<Id<T>> identifyIncompatibilityGroups(final JointPlans jointPlans, final Plan plan) {
		final JointPlan jp = jointPlans.getJointPlan( plan );
		return jp == null ?
			identifyIncompatibilityGroups( plan ) :
			identifyIncompatibilityGroups( jp );
	}
}

