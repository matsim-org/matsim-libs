/* *********************************************************************** *
 * project: org.matsim.*
 * SplitJointPlansBasedOnJointTripsModule.java
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
package playground.thibautd.socnetsim.replanning.modules;

import org.matsim.core.replanning.ReplanningContext;

import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
public class SplitJointPlansBasedOnJointTripsModule extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
	private final JointPlanFactory factory;

	public SplitJointPlansBasedOnJointTripsModule(
			final JointPlanFactory factory,
			final int nThreads) {
		super( nThreads );
		this.factory = factory;
	}

	@Override
	public GenericPlanAlgorithm<GroupPlans> createAlgorithm(ReplanningContext replanningContext) {
		return new SplitJointPlansBasedOnJointTripsAlgorithm(
				factory);
	}
}

