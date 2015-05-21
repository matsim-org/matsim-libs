/* *********************************************************************** *
 * project: org.matsim.*
 * ParetoWeight.java
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

import org.matsim.api.core.v01.population.Plan;

import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public class ParetoWeight implements WeightCalculator {
	private final WeightCalculator weight;

	public ParetoWeight(final WeightCalculator weight) {
		this.weight = weight;
	}

	@Override
	public double getWeight(
			final Plan indivPlan,
			final ReplanningGroup replanningGroup) {
		// the highest the rank, the better the plan
		final int rank = rank( indivPlan , replanningGroup );
		final int groupSize = replanningGroup.getPersons().size();
		return weight( rank , groupSize );
	}

	private static double weight(
			final int rank,
			final int groupSize) {
		// the idea is that W(i) > N * W(i-1)
		// with:
		//  - W(i) the weight of a plan of rank i
		//  - N the size of the group
		// this means that maximizing the sum of those weights
		// "lexicographically maximizes" the number of plans of a given
		// rank (ie it maximizes first the number of first-best plans,
		// then the number of second-best plans, and so on).
		// Such a solution is Pareto-optimal (it is even somehow a "good"
		// Pareto optimum)
		assert rank >= 0 : rank;
		if ( rank == 0 ) return 1; 

		final double weightLowestRank = weight( rank - 1 , groupSize );
		assert weightLowestRank > 0 : weightLowestRank;
		return (groupSize + 1) * weightLowestRank;
	}

	private int rank(
			final Plan indivPlan,
			final ReplanningGroup replanningGroup) {
		final double w = weight.getWeight( indivPlan , replanningGroup );
		int nWorsePlans = 0;

		for ( Plan otherPlan : indivPlan.getPerson().getPlans() ) {
			if ( otherPlan == indivPlan ) continue;
			final double otherWeight = weight.getWeight( otherPlan , replanningGroup );
			if ( otherWeight < w ) nWorsePlans++;
		}

		return nWorsePlans;
	}
}

