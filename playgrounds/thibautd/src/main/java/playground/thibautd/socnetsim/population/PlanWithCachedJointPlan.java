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
	private JointPlan jointPlan = null;

	public PlanWithCachedJointPlan( final Person person ) {
		super( person );
	}

	public JointPlan getJointPlan() {
		return jointPlan;
	}

	public void setJointPlan(final JointPlan jp) {
		// check if this was already called
		if ( this.jointPlanSet && jp != this.jointPlan ) throw new IllegalStateException();
		this.jointPlan = jp;
		this.jointPlanSet = true;
	}

	// should not be useful, but necessary for the remove method of JointPlans
	// to be well behaved
	public void resetJointPlan() {
		this.jointPlan = null;
		this.jointPlanSet = false;
	}
}

