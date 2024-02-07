/* *********************************************************************** *
 * project: org.matsim.*
 * PlansConfigGroup.java
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

package org.matsim.core.config.groups;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.Time;

public final class TimeAllocationMutatorConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "timeAllocationMutator";


	public TimeAllocationMutatorConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(MUTATION_RANGE, "Default:1800.0; Defines how many seconds a time mutation can maximally shift a time.");
		comments.put(MUTATION_AFFECTS_DURATION, "Default:true; Defines whether time mutation changes an activity's duration.");
		comments.put(LATEST_ACTIVITY_END_TIME,"Latest Activity End Time. Default = 24:00:00");
		comments.put(MUTATION_RANGE_STEP,"Mutation Range Step, default = 1 second");
		comments.put(MUTATE_AROUND_INITIAL_END_TIME_ONLY,"Mutates times only around the initially defined end times.");
		return comments;
	}

	// ---

	private static final String LATEST_ACTIVITY_END_TIME = "latestActivityEndTime";
	private static final String MUTATE_AROUND_INITIAL_END_TIME_ONLY = "mutateAroundInitialEndTimeOnly";
	private static final String MUTATION_RANGE_STEP = "mutationRangeStep";
	private static final String MUTATION_RANGE = "mutationRange";
	private double mutationRange = 1800.0;
	private double latestActivityEndTime = 24*3600.0;
	private boolean mutateAroundInitialEndTimeOnly = false;
	private double mutationRangeStep = 1.0;

	@StringGetter(MUTATION_RANGE_STEP)
	public double getMutationRangeStep() {
		return mutationRangeStep;
	}
	@StringSetter(MUTATION_RANGE_STEP)
	public void setMutationRangeStep(double mutationRangeStep) {
		this.mutationRangeStep = mutationRangeStep;
	}
	@StringGetter(LATEST_ACTIVITY_END_TIME)
	public double getLatestActivityEndTime() {
		return latestActivityEndTime;
	}
	@StringSetter(LATEST_ACTIVITY_END_TIME)
	public void setLatestActivityEndTime(String latestActivityEndTime) {
		this.latestActivityEndTime = Time.parseTime(latestActivityEndTime);
	}

	@StringGetter(MUTATE_AROUND_INITIAL_END_TIME_ONLY)
	public boolean isMutateAroundInitialEndTimeOnly() {
		return mutateAroundInitialEndTimeOnly;
	}
	@StringSetter(MUTATE_AROUND_INITIAL_END_TIME_ONLY)
	public void setMutateAroundInitialEndTimeOnly(boolean mutateAroundInitialEndTimeOnly) {
		this.mutateAroundInitialEndTimeOnly = mutateAroundInitialEndTimeOnly;
	}

	@StringGetter(MUTATION_RANGE)
	public double getMutationRange() {
		return this.mutationRange;
	}
	@StringSetter(MUTATION_RANGE)
	public void setMutationRange(final double val) {
		this.mutationRange = val;
	}

	// ---

	private static final String MUTATION_AFFECTS_DURATION = "mutationAffectsDuration";
	private boolean affectingDuration = true;
	@StringGetter(MUTATION_AFFECTS_DURATION)
	public boolean isAffectingDuration() {
		return this.affectingDuration;
	}
	@StringSetter(MUTATION_AFFECTS_DURATION)
	public void setAffectingDuration(boolean affectingDuration) {
		this.affectingDuration = affectingDuration;
	}

	// ---
}
