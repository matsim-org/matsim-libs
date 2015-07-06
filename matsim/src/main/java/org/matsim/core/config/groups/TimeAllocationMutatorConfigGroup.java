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

public final class TimeAllocationMutatorConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "TimeAllocationMutator";
	
	

	public TimeAllocationMutatorConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(MUTATION_RANGE, "Default:1800.0; Defines how many seconds a time mutation can maximally shift a time."); 
		return comments;
	}
	
	// ---

	private static final String MUTATION_RANGE = "mutationRange";
	private double mutationRange = 1800.0;
	@StringGetter(MUTATION_RANGE)
	public double getMutationRange() {
		return this.mutationRange;
	}
	@StringSetter(MUTATION_RANGE)
	public void setMutationRange(final double val) {
		this.mutationRange = val;
	}
	
	// ---
	
	private static final String MUTATION_AFFECTS_DURATION = "mutationAffectsDuration" ;
	private boolean affectingDuration = true ;
	@StringGetter(MUTATION_AFFECTS_DURATION)
	public boolean isAffectingDuration() {
		return affectingDuration;
	}
	@StringSetter(MUTATION_AFFECTS_DURATION)
	public void setAffectingDuration(boolean affectingDuration) {
		this.affectingDuration = affectingDuration;
	}

	// ---

}
