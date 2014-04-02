/* *********************************************************************** *
 * project: org.matsim.*
 * PlanWithCachedJointPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.population;

import java.util.Arrays;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PlanImpl;

/**
 * For performance reasons.
 * Using this class is by no means mandatory.
 *
 * @author thibautd
 */
final class PlanWithCachedJointPlan extends PlanImpl {
	private boolean jointPlanSet = false;

	private JointPlan[] jointPlans = null;

	public PlanWithCachedJointPlan( final Person person ) {
		super( person );
	}

	public JointPlan getJointPlan(final int containerId ) {
		if ( jointPlans == null ) return null;
		if ( jointPlans.length - 1 < containerId ) return null;
		return jointPlans[ containerId ];
	}

	public void setJointPlan(
			final int containerId,
			final JointPlan jp) {
		if ( jointPlans == null ) {
			jointPlans = new JointPlan[ containerId  + 1 ];
		}
		else if ( jointPlans.length - 1 < containerId ) {
			jointPlans = Arrays.copyOf( jointPlans , containerId + 1 );
		}

		// check if this was already called
		if ( this.jointPlans[ containerId ] != null &&
				jp != this.jointPlans[ containerId ] ) throw new IllegalStateException();

		this.jointPlans[ containerId ] = jp;
	}

	// should not be useful, but necessary for the remove method of JointPlans
	// to be well behaved
	public void resetJointPlan() {
		this.jointPlans = null;
	}
}

