/* *********************************************************************** *
 * project: org.matsim.*
 * PrismicLocationChoiceModule.java
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
package org.matsim.contrib.socnetsim.jointactivities.replanning.modules.prismiclocationchoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.ReplanningContext;

import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;

/**
 * @author thibautd
 */
public class PrismicLocationChoiceModule  extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
	private final Scenario scenario;
	private Provider<TripRouter> tripRouterProvider;

	public PrismicLocationChoiceModule(final Scenario sc, Provider<TripRouter> tripRouterProvider) {
		super( sc.getConfig().global() );
		this.scenario = sc;
		this.tripRouterProvider = tripRouterProvider;
	}

	@Override
	public GenericPlanAlgorithm<GroupPlans> createAlgorithm(final ReplanningContext replanningContext) {
		return new PrismicLocationChoiceAlgorithm(
				(PrismicLocationChoiceConfigGroup) scenario.getConfig().getModule( PrismicLocationChoiceConfigGroup.GROUP_NAME ),
				scenario.getActivityFacilities(),
				(SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME ),
				JointActingTypes.JOINT_STAGE_ACTS );
	}
}

