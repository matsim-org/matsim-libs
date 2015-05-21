/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.socnetsim.framework.replanning.selectors;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;

class FewGroupsIncompatibilityFactory implements IncompatiblePlansIdentifierFactory {

	@Override
	public IncompatiblePlansIdentifier createIdentifier(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final Set<JointPlan> knownJointPlans = new HashSet<JointPlan>();
		final IncompatiblePlansIdentifierImpl identifier = new IncompatiblePlansIdentifierImpl();

		for ( Person person : group.getPersons() ) {
			int i = 0;
			for ( Plan plan : person.getPlans() ) {
				final JointPlan jp = jointPlans.getJointPlan( plan );
				final int groupNr =  i++ % 3;

				if ( groupNr == 0 ) continue;
				final Set<Id<Person>> groups =
						Collections.<Id<Person>>singleton(
							Id.create( groupNr , Person.class ) );
				if ( jp == null ) {
					identifier.put(
							plan,
							groups );
				}
				else if ( !knownJointPlans.add( jp ) ) {
					for ( Plan p : jp.getIndividualPlans().values() ) {
						identifier.put(
								p,
								groups );
					}
				}
			}
		}

		return identifier;
	}
}
