/* *********************************************************************** *
 * project: org.matsim.*
 * PlanMutateTimeAllocation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population.algorithms;

import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;

/**
 * Mutates the duration of activities randomly within a specified range.
 * <br>
 * Other than the "full" version, this version just mutates activity end time and activity duration if they are defined,
 * without thinking any further.  If this produces invalid plans, they will eventually be removed through the selection
 * process.  kai, jun'12
 *
 * @author knagel
 */
public final class PlanMutateTimeAllocationSimplified implements PlanAlgorithm {

	private final StageActivityTypes blackList;
	private final double mutationRange;
	private final Random random;
	private final boolean affectingDuration;

	/**
	 * Initializes an instance mutating all activities in a plan
	 * @param mutationRange
	 * @param affectingDuration TODO
	 * @param random
	 */
	public PlanMutateTimeAllocationSimplified(final double mutationRange, boolean affectingDuration, final Random random) {
		this( EmptyStageActivityTypes.INSTANCE , mutationRange , affectingDuration, random );
	}
	/**
	 * Initializes an instance mutating all non-stage activities in a plan
	 * @param mutationRange
	 * @param affectingDuration TODO
	 * @param random
	 */
	public PlanMutateTimeAllocationSimplified(final StageActivityTypes blackList, final double mutationRange, boolean affectingDuration, final Random random) {
		this.blackList = blackList;
		this.mutationRange = mutationRange;
		this.affectingDuration = affectingDuration;
		this.random = random;
	}

	@Override
	public void run(final Plan plan) {
		for ( Activity act : TripStructureUtils.getActivities( plan , blackList ) ) {
			// this is deliberately simplistic.  Cleanup up of the time information should be done somewhere else.
			if (act.getEndTime() != Time.UNDEFINED_TIME) {
				act.setEndTime(mutateTime(act.getEndTime()));
			}
			if ( affectingDuration ) {
				if (act.getMaximumDuration() != Time.UNDEFINED_TIME) {
					act.setMaximumDuration(mutateTime(act.getMaximumDuration()));
				}
			}
		}
		// the legs are not doing anything. kai, jun'12
	}

	private double mutateTime(final double time) {
		double t = time;
		t = t + (int)((this.random.nextDouble() * 2.0 - 1.0) * this.mutationRange);

		if (t < 0) {
			t = 0;
		}
		// note that this also affects duration
		
		return t;
	}

}
