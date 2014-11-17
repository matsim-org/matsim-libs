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

package playground.sergioo.ptsim2013.replanning;

import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Mutates the duration of activities randomly within a specified range.
 * <br/>
 * Other than the "full" version, this version just mutates activity end time and activity duration if they are defined,
 * without thinking any further.  If this produces invalid plans, they will eventually be removed through the selection
 * process.  kai, jun'12
 *
 * @author knagel
 */
public class PlanMutateEndFirstActivity implements PlanAlgorithm {

	private final double mutationRange;
	private final Random random;
	private final Map<Id<Person>, Double> originalTimes;
	/**
	 * Initializes an instance mutating all non-stage activities in a plan
	 * @param mutationRange
	 * @param random
	 */
	public PlanMutateEndFirstActivity(final Map<Id<Person>, Double> originalTimes, final double mutationRange, final Random random) {
		this.originalTimes = originalTimes;
		this.mutationRange = mutationRange;
		this.random = random;
	}

	@Override
	public void run(final Plan plan) {
		for ( PlanElement planElement:plan.getPlanElements() ) {
			if(planElement instanceof Activity) {
				Activity act = (Activity)planElement;
				if (act.getEndTime() != Time.UNDEFINED_TIME) {
					act.setEndTime(mutateTime(originalTimes.get(plan.getPerson().getId())));
					if (act.getMaximumDuration() != Time.UNDEFINED_TIME)
						act.setMaximumDuration(act.getEndTime());
				}
				return;
			}
		}
	}

	private double mutateTime(double t) {
		t = t - (int)(this.random.nextDouble() * this.mutationRange);
		if (t < 0)
			t = 0;
		return t;
	}

}
