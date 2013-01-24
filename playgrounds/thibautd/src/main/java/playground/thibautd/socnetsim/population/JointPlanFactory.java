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
package playground.thibautd.socnetsim.population;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimFactory;

import playground.thibautd.socnetsim.scoring.HomogeneousScoreAggregator;
import playground.thibautd.socnetsim.scoring.ScoresAggregator;

/**
 * <b>Static</b> factory to create joint plans.
 * The fact that it is static allows to track which joint plan is associated
 * to each individual plan in a global way.
 *
 * @author thibautd
 */
public class JointPlanFactory implements MatsimFactory {
	public JointPlan createJointPlan(
			final Map<Id, ? extends Plan> plans) {
		return createJointPlan( plans, true );
	}

	/**
	 * equivalent to JointPlan(clique, plans, addAtIndividualLevel, true)
	 */
	public JointPlan createJointPlan(
			final Map<Id, ? extends Plan> plans,
			final boolean addAtIndividualLevel) {
		return createJointPlan( plans, addAtIndividualLevel, new HomogeneousScoreAggregator());
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
			final Map<Id, ? extends Plan> plans,
			final boolean addAtIndividualLevel,
			final ScoresAggregator aggregator) {
		JointPlan jointPlan = new JointPlan( plans, addAtIndividualLevel , aggregator );
		return jointPlan;
	}

	public JointPlan copyJointPlan(
			final JointPlan toCopy) {
		JointPlan copy = new JointPlan( toCopy );
		return copy;
	}
}

