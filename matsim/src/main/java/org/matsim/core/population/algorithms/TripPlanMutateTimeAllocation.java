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

package org.matsim.core.population.algorithms;

import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Copy/Paste of PlanMutateTimeAllocation, but with special handling
 * for stage activities (eg transit interaction activities, like line changes):
 * they are just ignored and not changed at all.
 *
 * @author mrieser
 */
public final class TripPlanMutateTimeAllocation implements PlanAlgorithm {

	public static final double DEFAULT_LATEST_END_TIME = 24. * 3600;

	private final double mutationRange;
	private final Random random;
	private boolean useActivityDurations = true;
	private final boolean affectingDuration;
//	private final String subpopulationAttribute;
	private final Map<String, Double> subpopulationMutationRanges;
	private final Map<String, Boolean> subpopulationAffectingDuration;
	private final double latestEndTime;

	public TripPlanMutateTimeAllocation(final double mutationRange,
			final boolean affectingDuration, final Random random) {
		this(mutationRange, affectingDuration, random, null, null );
	}

	public TripPlanMutateTimeAllocation(final double mutationRange, final boolean affectingDuration,
			final Random random, final Map<String, Double> subpopulationMutationRanges,
			final Map<String, Boolean> subpopulationAffectingDuration) {
		this(mutationRange, affectingDuration, random, subpopulationMutationRanges, subpopulationAffectingDuration,
				DEFAULT_LATEST_END_TIME);
	}

	public TripPlanMutateTimeAllocation(final double mutationRange, final boolean affectingDuration,
			final Random random, final Map<String, Double> subpopulationMutationRanges,
			final Map<String, Boolean> subpopulationAffectingDuration, final double latestEndTime) {
		this.mutationRange = mutationRange;
		this.affectingDuration = affectingDuration;
		this.random = random;
		this.subpopulationMutationRanges = subpopulationMutationRanges;
		this.subpopulationAffectingDuration = subpopulationAffectingDuration;
		this.latestEndTime = latestEndTime;
	}

	@Override
	public void run(final Plan plan) {
		mutatePlan(plan);
	}

	private void mutatePlan(final Plan plan) {

		double now = 0;
		boolean isFirst = true;
		Activity lastAct = (Activity) plan.getPlanElements().listIterator(plan.getPlanElements().size()).previous();

		final String subpopulation = this.getSubpopulation(plan);
		final boolean affectingDuration = this.isAffectingDuration(subpopulation);
		final double mutationRange = this.getMutationRange(subpopulation);

		// apply mutation to all activities except the last home activity
		for (PlanElement pe : plan.getPlanElements()) {

			if (pe instanceof Activity act) {

				// handle first activity
				if (isFirst) {
					isFirst = false;
					// set start to midnight
					act.setStartTime(now);
					// mutate the end time of the first activity
					act.setEndTime(mutateTime(act.getEndTime(), mutationRange));
					// calculate resulting duration
					if (affectingDuration) {
						act.setMaximumDuration(act.getEndTime().seconds() - act.getStartTime().seconds());
					}
					// move now pointer
					now += act.getEndTime().seconds();

				// handle middle activities
				} else if (act != lastAct) {

					// assume that there will be no delay between arrival time and activity start time
					act.setStartTime(now);
					if (!StageActivityTypeIdentifier.isStageActivity(act.getType())) {
						if (this.useActivityDurations) {
							if (act.getMaximumDuration().isDefined()) {
								// mutate the durations of all 'middle' activities
								if (affectingDuration) {
									act.setMaximumDuration(mutateTime(act.getMaximumDuration(), mutationRange));
								}
								now += act.getMaximumDuration().seconds();
								// (may feel a bit disturbing since it was not mutated but it is just using the "old" value which is perfectly ok. kai, jan'14)

								// set end time accordingly
								act.setEndTime(now);
							} else {
								double newEndTime = mutateTime(act.getEndTime(), mutationRange);
								if (newEndTime < now) {
									newEndTime = now;
								}
								act.setEndTime(newEndTime);
								now = newEndTime;
							}
						}
						else {
							if (act.getEndTime().isUndefined()) {
								throw new IllegalStateException("Can not mutate activity end time because it is not set for Person: " + plan.getPerson().getId());
							}
							double newEndTime = mutateTime(act.getEndTime(), mutationRange);
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
					// invalidate duration and end time because the plan will be interpreted
					// this.latestEndTime hour wrap-around
					act.setMaximumDurationUndefined();
					act.setEndTimeUndefined();
				}

			} else {
				Leg leg = (Leg) pe;

				// assume that there will be no delay between end time of previous activity and departure time
				leg.setDepartureTime(now);
				// let duration untouched. if defined add it to now
				if (leg.getTravelTime().isDefined()) {
					now += leg.getTravelTime().seconds();
				}
				final double arrTime = now;
				// set planned arrival time accordingly
				leg.setTravelTime( arrTime - leg.getDepartureTime().seconds());
			}
		}
	}

	private double mutateTime(final OptionalTime time, final double mutationRange) {
		if (time.isDefined()) {
			double t = time.seconds() + (int)((this.random.nextDouble() * 2.0 - 1.0) * mutationRange);
			if (t < 0)
				t = 0;
			if (t > this.latestEndTime)
				t = this.latestEndTime;
			return t;
		} else {
			return this.random.nextInt((int) this.latestEndTime);
		}
	}

	public void setUseActivityDurations(final boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
	}

	private String getSubpopulation(final Plan plan) {
		if (plan.getPerson() == null) return null;
		return PopulationUtils.getSubpopulation( plan.getPerson() );
	}

	private boolean isAffectingDuration(final String subpopulation) {
		if (subpopulation != null) {
			Boolean isAffectingDuration = this.subpopulationAffectingDuration.get(subpopulation);
			if (isAffectingDuration != null) return isAffectingDuration.booleanValue();
		}

		// fallback solution: no subpopulation attribute was found
		return this.affectingDuration;
	}

	private double getMutationRange(final String subpopulation) {
		if (subpopulation != null) {
			Double mutationRange = this.subpopulationMutationRanges.get(subpopulation);
			if (mutationRange != null) return mutationRange.doubleValue();
		}

		// fallback solution: no subpopulation attribute was found
		return this.mutationRange;
	}
}
