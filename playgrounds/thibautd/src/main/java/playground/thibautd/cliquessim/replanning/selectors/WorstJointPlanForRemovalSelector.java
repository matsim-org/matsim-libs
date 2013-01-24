/* *********************************************************************** *
 * project: org.matsim.*
 * WorstJointPlanForRemoval.java
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
package playground.thibautd.cliquessim.replanning.selectors;

import java.util.Collections;
import java.util.Comparator;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.cliquessim.population.Clique;

/**
 * Returns the plan with the lowest score.
 *
 * @author thibautd
 */
public class WorstJointPlanForRemovalSelector implements PlanSelector {

	@Override
	public Plan selectPlan(final Person person) {
		if (person instanceof Clique) return selectPlan((Clique) person);

		throw new IllegalArgumentException("WorstJointPlanForRemoval used "+
				"for a non clique agent");
	}

	private static Plan selectPlan(final Clique clique) {
		return Collections.min(
				clique.getPlans(),
				new ScoreComparator());
	}
}

class ScoreComparator implements Comparator<Plan> {
	@Override
	public int compare(final Plan plan1, final Plan plan2) {
		return Double.compare( plan1.getScore() , plan2.getScore() );
	}
}
