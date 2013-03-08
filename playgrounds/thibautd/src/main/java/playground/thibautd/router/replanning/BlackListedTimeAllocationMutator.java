/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutator.java
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
package playground.thibautd.router.replanning;

import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * A time allocation mutator to use with multi-leg routing.
 *
 * @author thibautd
 */
public class BlackListedTimeAllocationMutator implements PlanAlgorithm {
	private final double mutationRange;
	private final StageActivityTypes blackList;
	private final Random random;
	private boolean useActivityDurations = false;

	public BlackListedTimeAllocationMutator(
			final StageActivityTypes blackList,
			final double mutationRange,
			final Random random) {
		this.blackList = blackList;
		this.mutationRange = mutationRange;
		this.random = random;
	}

	@Override
	public void run(final Plan plan) {
		for ( Activity a : TripStructureUtils.getActivities( plan , blackList ) ) {
			if ( useActivityDurations ) {
				((ActivityImpl) a).setMaximumDuration( mutateTime( a.getMaximumDuration() ) );
			}
			else {
				a.setEndTime( mutateTime( a.getEndTime() ) );
			}
		}
	}

	private double mutateTime(final double time) {
		// do not do anything if time is undefined
		if ( time == Time.UNDEFINED_TIME ) return time;

		final double t = time + (int)((this.random.nextDouble() * 2.0 - 1.0) * this.mutationRange);
		return t < 0 ? 0 : t;
	}

	public void setUseActivityDurations(final boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
	}
}

