/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanFactory.java
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
package org.matsim.contrib.socnetsim.framework.population;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * <b>Static</b> factory to create joint plans.
 * The fact that it is static allows to track which joint plan is associated
 * to each individual plan in a global way.
 *
 * @author thibautd
 */
public class JointPlanFactory implements MatsimFactory {
	public JointPlan createJointPlan(
			final Map<Id<Person>, ? extends Plan> plans) {
		return createJointPlan( plans, true );
	}

	/**
	 * Creates a joint plan from individual plans.
	 * Two individual trips to be shared must have their Pick-Up activity type set
	 * to 'pu_i', where i is an integer which identifies the joint trip.
	 * @param plans the individual plans. If they consist of Joint activities, 
	 * those activities are referenced, otherwise, they are copied in a joint activity.
	 * @param addAtIndividualLevel if true, the plans are added to the Person's plans.
	 * set to false for a temporary plan (in a replaning for example).
	 */
	public JointPlan createJointPlan(
			final Map<Id<Person>, ? extends Plan> plans,
			final boolean addAtIndividualLevel) {
		JointPlan jointPlan = new JointPlan( plans );

		for (Plan plan : plans.values()) {
			if (addAtIndividualLevel) {
				final Person person = plan.getPerson();
				if (person == null) {
					throw new NullPointerException(
							"the person backpointed by the plan"+
							" must not be null for the plan to be added" );
				}
				if ( !person.getPlans().contains( plan ) ) person.addPlan( plan );
			}
		}

		return jointPlan;
	}

	public JointPlan copyJointPlan(
			final JointPlan toCopy) {
		return copyJointPlan( toCopy , true );
	}

	public JointPlan copyJointPlan(
			final JointPlan toCopy,
			final boolean addAtIndividualLevel) {
		return createJointPlan(
				cloneIndividualPlans( toCopy ),
				addAtIndividualLevel );
	}

	private static Map<Id<Person>, Plan> cloneIndividualPlans(final JointPlan plan) {
		final Map<Id<Person> , Plan> plans = new LinkedHashMap< >();

		for (Map.Entry<Id<Person>, Plan> indiv : plan.getIndividualPlans().entrySet()) {
			final Plan newPlan = createIndividualPlan( indiv.getValue().getPerson() );
			((PlanWithCachedJointPlan)newPlan).copyFrom( indiv.getValue() );
			// I think that the above cast will now fail.  It probably worked originally, since PlanWithCachedJointPlan was an extension of PlanImpl, which is
			// no longer allowed. I would, however, say that it was not a clean copy anyways, since it ignored the additional fields
			// of JointPlan.  Thibaut, I am confident that you can resolve this if you encounter it, but please let me know if you want to discuss.
			// kai, nov'15
			plans.put( indiv.getKey() , newPlan );
		}
		
		return plans;
	}

	public static Plan createIndividualPlan( final Person person) {
		return new PlanWithCachedJointPlan( person );
	}
}

