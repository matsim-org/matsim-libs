/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitySequenceMutatorAlgorithm.java
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
package org.matsim.contrib.socnetsim.framework.replanning.modules;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;
import java.util.Random;

/**
 * This algorithm must be followed by a re-route!
 *
 * @author thibautd
 */
public class ActivitySequenceMutatorAlgorithm implements PlanAlgorithm {
	private final Random random;
	private final StageActivityTypes stageTypes;

	public ActivitySequenceMutatorAlgorithm(
			final Random random,
			final StageActivityTypes stageTypes) {
		this.random = random;
		this.stageTypes = stageTypes;
	}

	@Override
	public void run(final Plan plan) {
		final List<Activity> activities = TripStructureUtils.getActivities( plan , stageTypes );
		// we need at least two activities in addition to the first/last
		if ( activities.size() < 4 ) return;

		// first act: not first nor last
		final int firstActIndex = 1 + random.nextInt( activities.size() - 2 );

		// second act: not first, last, nor first act
		final int secondActIndex = 1 + random.nextInt( activities.size() - 3 );

		swap( plan,
				activities.get( firstActIndex ),
				activities.get(
					secondActIndex < firstActIndex ?
					secondActIndex :
					secondActIndex + 1 ) );
	}

	private static void swap(
			final Plan plan,
			final Activity activity1,
			final Activity activity2) {
		final int index1 = plan.getPlanElements().indexOf( activity1 );
		final int index2 = plan.getPlanElements().indexOf( activity2 );

		// activities are not first nor last nor identical
		assert index1 > 0;
		assert index1 < plan.getPlanElements().size() - 2;
		assert index2 > 0;
		assert index2 < plan.getPlanElements().size() - 2;
		assert index1 != index2;

		plan.getPlanElements().set( index1 , activity2 );
		plan.getPlanElements().set( index2 , activity1 );

		// activities are now swapped
		assert plan.getPlanElements().get( index1 ) == activity2;
		assert plan.getPlanElements().get( index2 ) == activity1;
	}
}

