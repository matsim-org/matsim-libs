/* *********************************************************************** *
 * project: org.matsim.*
 * MutateActivityLocationsToLocationsOfOthersModule.java
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
package playground.thibautd.socnetsim.jointactivities.replanning.modules;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;

import playground.thibautd.socnetsim.framework.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.framework.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.jointactivities.replanning.modules.MutateActivityLocationsToLocationsOfOthersAlgorithm.ChoiceSet;

/**
 * @author thibautd
 */
@Deprecated
public class MutateActivityLocationsToLocationsOfOthersModule extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
	private final ChoiceSet choiceSet;

	public MutateActivityLocationsToLocationsOfOthersModule(
			final int nThreads,
			final Population population,
			final String type) {
		this( nThreads , new ChoiceSet( population , type ) );
	}

	public MutateActivityLocationsToLocationsOfOthersModule(
			final int nThreads,
			final ChoiceSet choiceSet) {
		super( nThreads );
		this.choiceSet = choiceSet;
	}

	@Override
	public GenericPlanAlgorithm<GroupPlans> createAlgorithm(ReplanningContext replanningContext) {
		return new MutateActivityLocationsToLocationsOfOthersAlgorithm(
				choiceSet,
				MatsimRandom.getLocalInstance());
	}
}

