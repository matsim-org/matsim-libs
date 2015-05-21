/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanMergingModule.java
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
package org.matsim.contrib.socnetsim.framework.replanning.modules;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;

import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;

/**
 * "widens" joint plans randomly. To use before
 * JointPlanAlgorithms which insert new interactions.
 * It does not considers any other "tie" information than the group.
 * Hence, if the group is not a clique, this may result in meaningless groupings.
 * @author thibautd
 */
public class JointPlanMergingModule extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
	private final double probAcceptance;
	private final JointPlanFactory factory;

	public JointPlanMergingModule(
			final JointPlanFactory factory,
			final int nThreads,
			final double probAcceptance) {
		super( nThreads );
		this.factory = factory;
		this.probAcceptance = probAcceptance;
	}

	@Override
	public GenericPlanAlgorithm<GroupPlans> createAlgorithm(ReplanningContext replanningContext) {
		return new JointPlanMergingAlgorithm(
				factory,
				probAcceptance,
				MatsimRandom.getLocalInstance() );
	}
}

