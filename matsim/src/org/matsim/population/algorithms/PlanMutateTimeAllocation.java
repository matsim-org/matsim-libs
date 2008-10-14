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

package org.matsim.population.algorithms;

import java.util.Random;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.utils.misc.Time;

/**
 * Mutates the duration of activities randomly within a specified range.
 * For the first act, the end-time is mutated. For the last act, duration and end-time are set to UNDEFINED.
 * For all other acts, the duration is mutated, and the end-time is updated accordingly.
 * If an activity has no duration set (UNDEFINED_TIME), a random time between 0 and 24h will be chosen.
 * Departure and arrival times of legs are updated according to the activity durations, but the
 * leg travel time is not modified (e.g. updated according to new expected departure time).
 *
 * @author mrieser
 */
public class PlanMutateTimeAllocation implements PlanAlgorithm {

	private final int mutationRange;
	private final Random random;

	public PlanMutateTimeAllocation(final int mutationRange, final Random random) {
		this.mutationRange = mutationRange;
		this.random = random;
	}

	public void run(final Plan plan) {
		mutatePlan(plan);
	}

	private void mutatePlan(final Plan plan) {

		int max = plan.getActsLegs().size();

		int now = 0;

		// apply mutation to all activities except the last home activity
		for (int i = 0; i < max; i++ ) {

			if (i % 2 == 0) {
				Act act = (Act)(plan.getActsLegs().get(i));
				// invalidate previous activity times because durations will change
				act.setStartTime(Time.UNDEFINED_TIME);

				// handle first activity
				if (i == 0) {
					// set start to midnight
					act.setStartTime(now);
					// mutate the end time of the first activity
					act.setEndTime(mutateTime(act.getEndTime()));
					// calculate resulting duration
					act.setDur(act.getEndTime() - act.getStartTime());
					// move now pointer
					now += act.getEndTime();

				// handle middle activities
				} else if (i < (max - 1)) {

					// assume that there will be no delay between arrival time and activity start time
					act.setStartTime(now);
					// mutate the durations of all 'middle' activities
					act.setDur(mutateTime(act.getDur()));
					now += act.getDur();
					// set end time accordingly
					act.setEndTime(now);

				// handle last activity
				} else {

					// assume that there will be no delay between arrival time and activity start time
					act.setStartTime(now);
					// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
					act.setDur(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);

				}

			} else {

				Leg leg = (Leg)(plan.getActsLegs().get(i));

				// assume that there will be no delay between end time of previous activity and departure time
				leg.setDepTime(now);
				// let duration untouched. if defined add it to now
				if (leg.getTravTime() != Time.UNDEFINED_TIME) {
					now += leg.getTravTime();
				}
				// set planned arrival time accordingly
				leg.setArrTime(now);

			}
		}
	}

	private double mutateTime(final double time) {
		double t = time;
		if (t != Time.UNDEFINED_TIME) {
			t = t + (int)((this.random.nextDouble() * 2.0 - 1.0) * this.mutationRange);
			if (t < 0) t = 0;
			if (t > 24*3600) t = 24*3600;
		} else {
			t = this.random.nextInt(24*3600);
		}
		return t;
	}

}
