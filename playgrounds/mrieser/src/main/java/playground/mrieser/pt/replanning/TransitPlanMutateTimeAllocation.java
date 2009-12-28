/* *********************************************************************** *
 * project: org.matsim.*
 * TransitPlanMutateTimeAllocation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.replanning;

import java.util.Random;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;


/**
 * Copy/Paste of PlanMutateTimeAllocation, but with special handling
 * for transit interaction activities (like line changes): they are
 * just ignored and not changed at all.
 *
 * @author mrieser
 */
public class TransitPlanMutateTimeAllocation implements PlanAlgorithm {

	private final int mutationRange;
	private final Random random;
	private boolean useActivityDurations;

	public TransitPlanMutateTimeAllocation(final int mutationRange, final Random random) {
		this.mutationRange = mutationRange;
		this.random = random;
	}

	public void run(final Plan plan) {
		mutatePlan(plan);
	}

	private void mutatePlan(final Plan plan) {

		double now = 0;
		boolean isFirst = true;
		ActivityImpl lastAct = (ActivityImpl) plan.getPlanElements().listIterator(plan.getPlanElements().size()).previous();

		// apply mutation to all activities except the last home activity
		for (PlanElement pe : plan.getPlanElements()) {

			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl)pe;

				// handle first activity
				if (isFirst) {
					isFirst = false;
					// set start to midnight
					act.setStartTime(now);
					// mutate the end time of the first activity
					act.setEndTime(mutateTime(act.getEndTime()));
					// calculate resulting duration
					act.setDuration(act.getEndTime() - act.getStartTime());
					// move now pointer
					now += act.getEndTime();

				// handle middle activities
				} else if (act != lastAct) {

					// assume that there will be no delay between arrival time and activity start time
					act.setStartTime(now);
					if (!act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
						if (this.useActivityDurations) {
							if (act.getDuration() != Time.UNDEFINED_TIME) {
								// mutate the durations of all 'middle' activities
								act.setDuration(mutateTime(act.getDuration()));
								now += act.getDuration();
								// set end time accordingly
								act.setEndTime(now);
							} else {
								double newEndTime = mutateTime(act.getEndTime());
								if (newEndTime < now) {
									newEndTime = now;
								}
								act.setEndTime(newEndTime);
								now = newEndTime;
							}
						}
						else {
							if (act.getEndTime() == Time.UNDEFINED_TIME) {
								throw new IllegalStateException("Can not mutate activity end time because it is not set for Person: " + plan.getPerson().getId());
							}
							double newEndTime = mutateTime(act.getEndTime());
							if (newEndTime < now) {
								newEndTime = now;
							}
							act.setEndTime(newEndTime);
							now = newEndTime;
						}
					}
				// handle last activity
				} else {

					// assume that there will be no delay between arrival time and activity start time
					act.setStartTime(now);
					// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
					act.setDuration(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);

				}

			} else {
				LegImpl leg = (LegImpl) pe;

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
			t = t + (int)((this.random.nextDouble() * 2.0 - 1.0) * this.mutationRange);
			if (t < 0) t = 0;
			if (t > 24*3600) t = 24*3600;
		} else {
			t = this.random.nextInt(24*3600);
		}
		return t;
	}

	public void setUseActivityDurations(final boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
	}

}
