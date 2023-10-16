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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;

/**
 * Mutates the duration of activities randomly within a specified range.
 *
 * @author knagel, jbischoff
 */
public final class MutateActivityTimeAllocation implements PlanAlgorithm {


	public static final String INITIAL_END_TIME_ATTRIBUTE = "initialEndTime";
	private final double mutationRange;
	private final Random random;
	private final boolean affectingDuration;
	private final double latestActivityEndTime;
	private final boolean mutateAroundInitialEndTimeOnly;
	private final double mutationRangeStep;


	public MutateActivityTimeAllocation(final double mutationRange, boolean affectingDuration, final Random random, double latestActivityEndTime, boolean mutateAroundInitialEndTimeOnly, double mutationRangeStep) {
		this.mutationRange = mutationRange;
		this.affectingDuration = affectingDuration;
		this.random = random;
		this.latestActivityEndTime = latestActivityEndTime;
		this.mutateAroundInitialEndTimeOnly = mutateAroundInitialEndTimeOnly;
		this.mutationRangeStep = Math.max(1.0,mutationRangeStep);

	}

	@Override
	public void run(final Plan plan) {
		for ( Activity act : TripStructureUtils.getActivities( plan , StageActivityHandling.ExcludeStageActivities ) ) {
			if (act.getEndTime().isDefined()) {
				double endTime = act.getEndTime().seconds();
				if (mutateAroundInitialEndTimeOnly){
					Object initialEndtime = act.getAttributes().getAttribute(INITIAL_END_TIME_ATTRIBUTE);
					if (initialEndtime!=null) {
						endTime = (double) initialEndtime;
					} else {
						act.getAttributes().putAttribute(INITIAL_END_TIME_ATTRIBUTE,endTime);
					}
				}
				double newEndTime = Math.min(mutateTime(endTime, mutationRange),this.latestActivityEndTime);
				act.setEndTime(newEndTime);
				act.setStartTimeUndefined();
			}
			else if ( affectingDuration ) {
				if ( act.getMaximumDuration().isDefined()) {
					act.setMaximumDuration(mutateTime(act.getMaximumDuration().seconds(), mutationRange));
				}
			}
		}
		setLegDepartureTimes(plan);

	}

	private void setLegDepartureTimes(Plan plan) {
		//setting leg departure times can only be an estimate and might be useful for certain dynamic modes.
		//In general, it is best to trigger a reroute after mutating time.
		double now = 0;
		for (PlanElement planElement : plan.getPlanElements()){
			if (planElement instanceof Activity activity){
				if (activity.getEndTime().isDefined()){
					now = activity.getEndTime().seconds();
				}
				else if (activity.getMaximumDuration().isDefined()){
					now = now + activity.getMaximumDuration().seconds();
				}
			} else if (planElement instanceof Leg leg){
				if (leg.getDepartureTime().isDefined()) {
					leg.setDepartureTime(now);
					if (leg.getTravelTime().isDefined()) {
						now = now + leg.getTravelTime().seconds();
					}
				}
			}
		}
	}

	private double mutateTime(final double time, double mutationRange) {
		double t = time;
		int mutationRangeBins = (int) Math.ceil( mutationRange/mutationRangeStep);
		t = t - mutationRange + (2*this.random.nextInt(mutationRangeBins)*mutationRangeStep) ;
		if (t < 0) {
			t = 0;
		}
		// note that this also affects duration
		return t;
	}



}
