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
package playground.thibautd.socnetsim.framework.population;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

/**
 * class for handling synchronized plans.
 * @author thibautd
 */
public class JointPlan {
	private final Map<Id<Person>,Plan> individualPlans = new LinkedHashMap< >();

	JointPlan( final Map<Id<Person>, ? extends Plan> plans ) {
		this.individualPlans.putAll( plans );
	}

	public Plan getIndividualPlan(final Id<Person> id) {
		return this.individualPlans.get(id);
	}

	public Map<Id<Person>,Plan> getIndividualPlans() {
		return this.individualPlans;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+": plans="+getIndividualPlans();
			//", isSelected="+this.isSelected();
	}
}
