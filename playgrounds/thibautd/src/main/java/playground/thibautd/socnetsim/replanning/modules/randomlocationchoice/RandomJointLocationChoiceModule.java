/* *********************************************************************** *
 * project: org.matsim.*
 * RandomJointLocationChoiceModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning.modules.randomlocationchoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.facilities.ActivityFacilities;

import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;

/**
 * @author thibautd
 */
public class RandomJointLocationChoiceModule extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
	private final RandomJointLocationChoiceConfigGroup config;
	private final ActivityFacilities facilities;
	private final SocialNetwork socialNetwork;

	public RandomJointLocationChoiceModule(final Scenario scenario) {
		this( scenario.getConfig().global().getNumberOfThreads(),
				(RandomJointLocationChoiceConfigGroup)
					scenario.getConfig().getModule(
						RandomJointLocationChoiceConfigGroup.GROUP_NAME ),
				scenario.getActivityFacilities(),
				(SocialNetwork)
					scenario.getScenarioElement(
						SocialNetwork.ELEMENT_NAME ) );
	}

	public RandomJointLocationChoiceModule(
			final int nThreads,
			final RandomJointLocationChoiceConfigGroup config,
			final ActivityFacilities facilities,
			final SocialNetwork socialNetwork) {
		super( nThreads );
		this.config = config;
		this.facilities = facilities;
		this.socialNetwork = socialNetwork;
	}

	@Override
	public GenericPlanAlgorithm<GroupPlans> createAlgorithm(ReplanningContext replanningContext) {
		return new RandomJointLocationChoiceAlgorithm( config , facilities , socialNetwork );
	}
}

