/* *********************************************************************** *
 * project: org.matsim.*
 * RandomPlanSelectorWithoutCasts.java
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
package playground.thibautd.jointtrips.replanning.selectors;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * A selector which selects a random plan, without requiring a PersonImpl
 * @author thibautd
 */
public class RandomPlanSelectorWithoutCasts implements PlanSelector {

	@Override
	public Plan selectPlan(final Person person) {
		List<? extends Plan> plans = person.getPlans();

		if ( plans.size() == 0 ) {
			return null;
		}

		int index = MatsimRandom.getRandom().nextInt( plans.size() );

		return plans.get(index);
	}
}

