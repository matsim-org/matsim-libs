/* *********************************************************************** *
 * project: org.matsim.*
 * PlanMutateTimeAllocation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu.bottleneck;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * it's a modified copy of org.matsim.population.algorithms.PlanMutateTimeAllocation
 *
 * @author yu
 */
public class PlanMutateTimeAllocationBottleneck implements PlanAlgorithm {
	//------------------------------MEMBER VARIABLE---------------------------
	private final int mutationRange;

	//---------------------------------CONSTRUCTOR----------------------------
	public PlanMutateTimeAllocationBottleneck(int mutationRange) {
		this.mutationRange = mutationRange;
	}

	public void run(Plan plan) {
		mutatePlan(plan);
	}

	@SuppressWarnings("deprecation")
	private void mutatePlan(Plan plan) {

		int max = plan.getPlanElements().size();

		int now = 0;

		// apply mutation to all activities except the last home activity
		for (int i = 0; i < max; i++) {

			if (i % 2 == 0) {
				ActivityImpl act = (ActivityImpl) (plan.getPlanElements().get(i));
				// invalidate previous activity times because durations will change
				act.setStartTime(Time.UNDEFINED_TIME);

				// handle first activity
				if (i == 0) {
					// set start to midnight
					act.setStartTime(now);
					// mutate the end time of the first activity
					act.setEndTime(mutateTime(act.getEndTime()));
					// calculate resulting duration
					act.setMaximumDuration(act.getEndTime() - act.getStartTime());
					// move now pointer
					now += act.getEndTime();

					// handle middle activities	
				} else if ((i > 0) && (i < (max - 1))) {

					// assume that there will be no delay between arrival time and activity start time
					act.setStartTime(now);
					// mutate the durations of all 'middle' activities
					act.setMaximumDuration(7200);
					//		   ^^^^
					now += act.getMaximumDuration();
					// set end time accordingly
					act.setEndTime(Time.UNDEFINED_TIME);
					//					^^^^^^^^^^^^^^^^^^
					// handle last activity
				} else if (i == (max - 1)) {

					// assume that there will be no delay between arrival time and activity start time
					act.setStartTime(now);
					// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
					act.setMaximumDuration(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);
				}

			} else {

				LegImpl leg = (LegImpl) (plan.getPlanElements().get(i));

				// assume that there will be no delay between end time of previous activity and departure time
				leg.setDepartureTime(now);
				// let duration untouched. if defined add it to now
				if (leg.getTravelTime() != Time.UNDEFINED_TIME) {
					now += leg.getTravelTime();
				}
				// set planned arrival time accordingly
				leg.setArrivalTime(now);

			}
		}
	}

	private double mutateTime(final double time) {
		double t = time;
		if (t != Time.UNDEFINED_TIME) {
			t = t
					+ (int) ((MatsimRandom.getRandom().nextDouble() * 2.0 - 1.0) * mutationRange);
			if (t < 0)
				t = 0;
			if (t > 24 * 3600)
				t = 24 * 3600;
		} else {
			t = MatsimRandom.getRandom().nextInt(24 * 3600);
		}
		return t;
	}
}
