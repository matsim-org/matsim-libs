/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.utils.CompactUnmodifiablePlanMap;

import java.util.Map;

/**
 * class for handling synchronized plans.
 * @author thibautd
 */
public class JointPlan {
	private final CompactUnmodifiablePlanMap individualPlans;

	JointPlan( final Map<Id<Person>, ? extends Plan> plans ) {
		this.individualPlans = new CompactUnmodifiablePlanMap( plans.values() );
	}

	public Plan getIndividualPlan(final Id<Person> id) {
		return this.individualPlans.get(id);
	}

	public Map<Id<Person>,Plan> getIndividualPlans() {
		return this.individualPlans;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder( getClass().getSimpleName()+": plans=" );
		for ( Plan p : getIndividualPlans().values() ) {
			builder.append(
				p.getPerson().getId()+": "+p.getPerson().getPlans().indexOf( p )+"; " );
		}
		return builder.toString();
	}
}
