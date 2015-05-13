/* *********************************************************************** *
 * project: org.matsim.*
 * LowestScoreOfJointPlanWeight.java
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
package playground.thibautd.socnetsim.replanning.selectors;

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public class LowestScoreOfJointPlanWeight implements WeightCalculator {
	private final WeightCalculator baseWeight;
	private final JointPlans jointPlans;


	public LowestScoreOfJointPlanWeight(
			final JointPlans jointPlans) {
		this( new ScoreWeight() , jointPlans );
	}

	public LowestScoreOfJointPlanWeight(
			final WeightCalculator baseWeight,
			final JointPlans jointPlans) {
		this.baseWeight = baseWeight;
		this.jointPlans = jointPlans;
	}

	@Override
	public double getWeight(
			final Plan indivPlan,
			final ReplanningGroup replanningGroup) {
		final JointPlan jointPlan = jointPlans.getJointPlan( indivPlan );

		if ( jointPlan == null ) return baseWeight.getWeight( indivPlan , replanningGroup );

		double minWeight = Double.POSITIVE_INFINITY;

		for ( Plan p : jointPlan.getIndividualPlans().values() ) {
			minWeight = Math.min(
					minWeight, 
					baseWeight.getWeight(
						p,
						replanningGroup ) );
		}

		return minWeight;
	}
}

